package es.um.sisdist.backend.Service.impl.tests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
//import com.tuempresa.grpc.PromptServiceGrpc;
//import com.tuempresa.grpc.PromptRequest;
//import com.tuempresa.grpc.PromptReply;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;

public class AppLogicImplTest {

    @Test
    void testFetchPromptResponseWithRealGrpc() throws Exception {
        // Configurar un servidor gRPC local
        Server server = ServerBuilder.forPort(50051)
                .addService(new PromptServiceGrpc.PromptServiceImplBase() {
                    @Override
                    public void sendPrompt(PromptRequest request, StreamObserver<PromptReply> responseObserver) {
                        // Responder con un mensaje simulado
                        PromptReply reply = PromptReply.newBuilder()
                                .setResponse("mock response")
                                .build();
                        responseObserver.onNext(reply);
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();

        // Crear una instancia real de GrpcServiceClient
        GrpcServiceClient client = new GrpcServiceClient("localhost", 50051);

        // Probar el método fetchPromptResponse
        String response = client.fetchPromptResponse("test prompt");
        assertEquals("mock response", response, "La respuesta del servicio gRPC no coincide");

        // Detener el servidor gRPC
        server.shutdown();
    }
}