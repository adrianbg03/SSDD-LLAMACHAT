package es.um.sisdist.backend.Service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

//import com.tuempresa.grpc.PromptServiceGrpc;
//import com.tuempresa.grpc.PromptRequest;
//import com.tuempresa.grpc.PromptReply;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import es.um.sisdist.backend.grpc.*;

public class PromptGrpcClient {

    private final GrpcServiceClient grpcServiceClient;

    public PromptGrpcClient() {
        // Crear una instancia de GrpcServiceClient
        grpcServiceClient = new GrpcServiceClient("backend-grpc", 50051);
        // grpcServiceClient = new GrpcServiceClient("localhost", 50051);
    }

    public String fetchAndResponse(String prompt) {
        // Delegar la llamada al GrpcServiceClient
        // return grpcServiceClient.fetchPromptResponse(prompt);
        grpcServiceClient.sendPromptAndFetchResponse(prompt);
        return "Hecho";
    }
}