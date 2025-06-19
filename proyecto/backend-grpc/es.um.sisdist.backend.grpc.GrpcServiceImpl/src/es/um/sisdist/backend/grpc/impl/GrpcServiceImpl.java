package es.um.sisdist.backend.grpc.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PingResponse;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import es.um.sisdist.backend.grpc.ResponseRequest;
import es.um.sisdist.backend.grpc.ResponseResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

class GrpcServiceImpl extends GrpcServiceGrpc.GrpcServiceImplBase {
    private Logger logger;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    public GrpcServiceImpl(Logger logger) {
        super();
        this.logger = logger;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        logger.info("Recived PING request, value = " + request.getV());
        responseObserver.onNext(PingResponse.newBuilder().setV(request.getV()).build());
        responseObserver.onCompleted();
    }

    /*
     * Metodo POST que envía el prompt como JSON al servicio REST /prompt
     * Recibe un codigo 202 Acceptedcon un token en la cabecera Location
     * Devuelve el token al cliente gRPC
     */
    @Override
    public void sendPrompt(PromptRequest request, StreamObserver<PromptResponse> responseObserver) {
        String promptText = request.getPrompt();
        logger.info("prompText que se envia a llamachat = " + promptText);

        // Crear la solicitud JSON con el prompt
        String json = "{\"prompt\": \"" + promptText + "\"}";
        logger.info("prompText que se envia a llamachat en json= " + json);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        // Leer y mostrar el contenido de RequestBody
        String bodyContent = readRequestBody(body);
        logger.info("Contenido del RequestBody: " + bodyContent);

        // Configurara la solicitud HTTP
        Request requestHttp = new Request.Builder()
                .url("http://ssdd-llamachat:5020/prompt")
                .post(body)
                .build();
        logger.info("Enviando solicitud POST a LlamaChat con URL: " + requestHttp.url());

        // Llamar a API LlamaChat
        try (Response response = httpClient.newCall(requestHttp).execute()) {
            int statusCode = response.code();
            String location = response.header("Location");
            logger.info("HA ENTRADO A sendPrompt del server status:" + statusCode + " location:" + location);
            if (statusCode == 202 && location != null && location.contains("/response/")) {
                // Obtener el token de la cabecera "Location"
                String token = location.substring(location.lastIndexOf("/") + 1);
                System.out.println("Token extraido = " + token);

                // Responder con el token
                PromptResponse promptResponse = PromptResponse.newBuilder()
                        .setToken(token)
                        .build();

                // Enviar respuesta solo si no se ha enviado antes
                responseObserver.onNext(promptResponse);
                responseObserver.onCompleted();
                // return;
            } else {
                // Enviar error si el código de respuesta HTTP no es 202
                // responseObserver.onError(new RuntimeException("Respuesta HTTP inesperada.
                // Código: " + statusCode));
                // logger.info("Respuesta HTTP inesperada. Código: " + statusCode);
                logger.warning("Respuesta HTTP inesperada. Código: " + statusCode);

                responseObserver.onError(
                        Status.FAILED_PRECONDITION
                                .withDescription("Respuesta HTTP inesperada. Código: " + statusCode)
                                .asRuntimeException());
            }
        } catch (IOException e) {
            // Enviar error en caso de excepción en la solicitud HTTP
            // responseObserver.onError(new RuntimeException("Error al procesar la solicitud
            // HTTP: " + e.getMessage(), e));
            logger.severe("Error al procesar la solicitud HTTP: " + e.getMessage());
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Error al procesar la solicitud HTTP")
                            .withCause(e)
                            .asRuntimeException());
        }
    }

    public static String readRequestBody(RequestBody body) {
        Buffer buffer = new Buffer();
        try {
            // Volcar el contenido de RequestBody en el buffer
            body.writeTo(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Convertir el buffer a una cadena
        return buffer.readUtf8();
    }

    /*
     * Metodo GET que consulta el estado de la respuesta /response/{token}
     * Si 204 No Content, sigue procesando
     * Si 200 Ok, extrae la respuesta
     */
    @Override
    public void getResponse(ResponseRequest request, StreamObserver<ResponseResponse> responseObserver) {
        String token = request.getToken();
        System.out.println("Token solicitado = " + token);

        while (true) {
            Request requestHttp = new Request.Builder()
                    .url("http://ssdd-llamachat:5020" + "/response/" + token)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(requestHttp).execute()) {
                int statusCode = response.code();
                String responseBody = response.body().string();

                System.out.println("Código de estado: " + response.code());
                System.out.println("Cuerpo de la respuesta: " + responseBody);

                switch (statusCode) {
                    case 102: // Processing - El servicio no está inicializado
                        logger.warning("El servicio aún no está inicializado. Reintentando en 2 segundos...");
                        aux_sleep();
                        continue;

                    case 202: // Accepted - Petición aceptada, pero aún sin respuesta
                        logger.info("Solicitud aceptada, pero la respuesta aún no está lista. Reintentando...");
                        aux_sleep();
                        continue;

                    case 204: // No content - La respuesta sigue en proceso
                        logger.info("La respuesta sigue en proceso. Reintentando en 2 segundos...");
                        aux_sleep();
                        continue;

                    case 200: // OK - Respuesta al prompt:
                        String answer = extractAnswer(responseBody);
                        ResponseResponse responseProto = ResponseResponse.newBuilder()
                                .setStatus("completed")
                                .setAnswer(answer)
                                .build();
                        responseObserver.onNext(responseProto);
                        responseObserver.onCompleted();
                        return; // Salir

                    case 400:
                        logger.severe("Solicitud incorrecta. Token inválido.");
                        responseObserver.onError(new RuntimeException("Error 400: Token inválido."));
                        return;

                    case 404:
                        logger.severe("Token no encontrado en el servidor.");
                        responseObserver.onError(new RuntimeException("Error 404: Token no encontrado."));
                        return;

                    case 500:
                        logger.severe("Error interno en el servidor llamachat.");
                        responseObserver.onError(new RuntimeException("Error 500: Fallo interno del servidor."));
                        return;

                    default: // Cualquier otro código de error no manejado
                        logger.severe("Código de estado inesperado: " + statusCode);
                        responseObserver.onError(new RuntimeException("Error inesperado. Código: " + statusCode));
                        return;
                }

            } catch (IOException e) {
                logger.severe("Error en la comunicación con el servidor: " + e.getMessage());
                responseObserver.onError(e);
                return;
            } catch (Exception e) {
                // Esta captura de excepciones es para cualquier otra excepción inesperada
                logger.severe("Error inesperado: " + e.getMessage());
                responseObserver.onError(e);
                return;
            }

        }

    }

    /*
     * Método auxiliar para extraer la respuesta de "answer" en una cadena JSON
     */
    private String extractAnswer(String jsonResponse) {
        // Objeto ObjectMapper de Jackson para manejar JSON
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse); // Convertit la cadena JSON en un objeto JsonNode
            return jsonNode.get("answer").asText(); // Devolver la respuesta como String
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error procesando JSON";
        }
    }

    private void aux_sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/*
 * @Override
 * public void storeImage(ImageData request, StreamObserver<Empty>
 * responseObserver)
 * {
 * logger.info("Add image " + request.getId());
 * imageMap.put(request.getId(),request);
 * responseObserver.onNext(Empty.newBuilder().build());
 * responseObserver.onCompleted();
 * }
 * 
 * @Override
 * public StreamObserver<ImageData> storeImages(StreamObserver<Empty>
 * responseObserver)
 * {
 * // La respuesta, sólo un objeto Empty
 * responseObserver.onNext(Empty.newBuilder().build());
 * 
 * // Se retorna un objeto que, al ser llamado en onNext() con cada
 * // elemento enviado por el cliente, reacciona correctamente
 * return new StreamObserver<ImageData>() {
 * 
 * @Override
 * public void onCompleted() {
 * // Terminar la respuesta.
 * responseObserver.onCompleted();
 * }
 * 
 * @Override
 * public void onError(Throwable arg0) {
 * }
 * 
 * @Override
 * public void onNext(ImageData imagedata)
 * {
 * logger.info("Add image (multiple) " + imagedata.getId());
 * imageMap.put(imagedata.getId(), imagedata);
 * }
 * };
 * }
 * 
 * @Override
 * public void obtainImage(ImageSpec request, StreamObserver<ImageData>
 * responseObserver) {
 * // TODO Auto-generated method stub
 * super.obtainImage(request, responseObserver);
 * }
 * 
 * @Override
 * public StreamObserver<ImageSpec> obtainCollage(StreamObserver<ImageData>
 * responseObserver) {
 * // TODO Auto-generated method stub
 * return super.obtainCollage(responseObserver);
 * }
 * 
 * }
 */