/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package es.um.sisdist.backend.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
//import io.grpc.StatusRuntimeException;
//import io.grpc.stub.StreamObserver;
//import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
//import com.google.protobuf.ByteString;
//import com.google.protobuf.Empty;

/**
 * A simple client that requests a greeting from the {@link CollageServer}.
 */
@Service // Asegura que Spring lo detecte como un Bean
public class GrpcServiceClient {

  private static final Logger logger = Logger.getLogger(GrpcServiceClient.class.getName());

  private final ManagedChannel channel;
  // private final GrpcServiceGrpc.GrpcServiceBlockingStub blockingStub;
  private final GrpcServiceGrpc.GrpcServiceStub asyncStub;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  // ----------------------------------------------------------
  // Para servicio REST Externo
  private static final String DEFAULT_HOST = "backend-grpc";
  private static final int DEFAULT_PORT = 50051;

  private volatile String lastResponse = "Aún no hay respuesta";

  public GrpcServiceClient() {
    this(DEFAULT_HOST, DEFAULT_PORT);
  }
  // ----------------------------------------------------------

  // Construct client connecting to HelloWorld server at {@code host:port}.
  public GrpcServiceClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid needing certificates.
        .usePlaintext()
        .build();
    // blockingStub = GrpcServiceGrpc.newBlockingStub(channel);
    asyncStub = GrpcServiceGrpc.newStub(channel); // Crear stub para llamadas asíncronas
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public void sendPromptAndFetchResponse(String prompt) {
    CountDownLatch latch = new CountDownLatch(1); // Espera a que se complete la respuesta
    logger.info("Cliente envia el prompt: " + prompt);
    final String[] grpcResponse = new String[1];
    // 1. Enviar el prompt y recibir el token de ese prompt
    asyncStub.sendPrompt(PromptRequest.newBuilder().setPrompt(prompt).build(),
        new StreamObserver<PromptResponse>() {

          @Override
          public void onNext(PromptResponse response) {
            String token = response.getToken();
            logger.info("Cliente recibe el Token de su prompt: " + token);

            // 2. Llamar a fetchResponse para obtener la respuesta
            fetchResponse(token, latch, grpcResponse);
          }

          @Override
          public void onCompleted() {
            logger.info("Prompt enviado correctamente.");
          }

          @Override
          public void onError(Throwable t) {
            logger.severe("Error en sendPrompt opopopp: " + t.getMessage());
            grpcResponse[0] = "Error: No se pudo obtener la respuesta del servicio gRPC";
            latch.countDown();
          }
        });

    try {
      latch.await(); // Esperar a que termine la respuesta del servidor gRPC
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void fetchResponse(String token, CountDownLatch latch, String[] grpcResponse) {
    logger.info("Cliente quiere consultar el estado del Token: " + token);
    asyncStub.getResponse(ResponseRequest.newBuilder().setToken(token).build(),
        new StreamObserver<ResponseResponse>() {

          @Override
          public void onNext(ResponseResponse response) {
            if ("completed".equals(response.getStatus())) {
              logger.info("Respuesta recibida: " + response.getAnswer());
              lastResponse = response.getAnswer(); // Guardamos la última respuesta
              grpcResponse[0] = response.getAnswer();
              latch.countDown(); // Solo se cuenta hacia abajo una vez
            } else if ("processing".equals(response.getStatus())) {
              logger.info("Esperando respuesta...");
              try {
                Thread.sleep(2000); // Esperar
                fetchResponse(token, latch, grpcResponse); // Reintentar la solicitud
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
          }

          @Override
          public void onError(Throwable t) {
            logger.severe("Error en getResponse: " + t.getMessage());
            t.printStackTrace();
            grpcResponse[0] = "Error al obtener respuesta del servidor";
            latch.countDown(); // Asegúrate de contar hacia abajo si ocurre un error
          }

          @Override
          public void onCompleted() {
            logger.info("Finalizada la obtención de respuesta.");
          }
        });
  }

  public String getLastResponse() {
    return lastResponse;
  }

  /*
   * public static void main(String[] args) throws InterruptedException {
   * GrpcServiceClient client = new GrpcServiceClient("localhost", 50051);
   * client.sendPromptAndFetchResponse("¿Cuál es la capital de Francia?");
   * client.shutdown();
   * }
   */

}

/*
 * // Send images.
 * public void sendImagesAndGetCollage()
 * {
 * // Imágenes para enviar
 * ImageData image1 = ImageData.newBuilder().setId("imagen1")
 * .setData(ByteString.copyFrom("Imagen 1 data".getBytes())).build();
 * ImageData image2 = ImageData.newBuilder().setId("imagen2")
 * .setData(ByteString.copyFrom("Imagen 2 data".getBytes())).build();
 * 
 * try {
 * blockingStub.storeImage(image1);
 * blockingStub.storeImage(image2);
 * } catch (StatusRuntimeException e) {
 * logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
 * return;
 * }
 * 
 * // Stream
 * try {
 * final CountDownLatch finishLatch = new CountDownLatch(1);
 * 
 * StreamObserver<Empty> soEmpty = new StreamObserver<Empty>() {
 * 
 * @Override
 * public void onNext(Empty value) {
 * }
 * 
 * @Override
 * public void onError(Throwable t) {
 * finishLatch.countDown();
 * }
 * 
 * @Override
 * public void onCompleted() {
 * finishLatch.countDown();
 * }
 * };
 * 
 * StreamObserver<ImageData> so = asyncStub.storeImages(soEmpty);
 * so.onNext(image1);
 * so.onNext(image2);
 * so.onCompleted();
 * 
 * // Esperar la respuesta
 * if (finishLatch.await(1, TimeUnit.SECONDS))
 * logger.info("Received response.");
 * else
 * logger.info("Not received response!");
 * 
 * } catch (StatusRuntimeException e) {
 * logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
 * return;
 * } catch (InterruptedException e) {
 * // TODO Auto-generated catch block
 * e.printStackTrace();
 * }
 * 
 * }
 * 
 * // * Collage client
 * public static void main(String[] args) throws Exception {
 * GrpcServiceClient client =
 * new GrpcServiceClient(args.length == 0 ? "localhost" : args[0],
 * 50051);
 * try {
 * // Access a service running on the local machine on port 50051
 * client.sendImagesAndGetCollage();
 * } finally {
 * client.shutdown();
 * }
 * }
 */
