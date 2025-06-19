
package es.um.sisdist.backend.Service.impl;

import java.util.Optional;
import java.util.logging.Logger;
import java.util.List;
import es.um.sisdist.backend.dao.DAOFactoryImpl;
import es.um.sisdist.backend.dao.IDAOFactory;
import es.um.sisdist.backend.dao.conversation.IConversationDAO;
import es.um.sisdist.backend.dao.models.Conversation;
import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.models.utils.UserUtils;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.backend.grpc.GrpcServiceClient;
import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import es.um.sisdist.backend.grpc.ResponseRequest;
import es.um.sisdist.backend.grpc.ResponseResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AppLogicImpl {
    private static final Logger logger = Logger.getLogger(AppLogicImpl.class.getName());
    private final ManagedChannel channel;
    private final GrpcServiceGrpc.GrpcServiceBlockingStub blockingStub;
    private String lastPromptResponse; // Almacena la última respuesta del prompt
    private final GrpcServiceClient grpcClient; // Instancia de GrpcServiceClient

    private static final AppLogicImpl instance = new AppLogicImpl();

    private final IDAOFactory daoFactory;
    private final IUserDAO dao;

    private final IConversationDAO conversationDao;

    private AppLogicImpl() {
        daoFactory = new DAOFactoryImpl();
        Optional<String> backend = Optional.ofNullable(System.getenv("DB_BACKEND"));
        grpcClient = new GrpcServiceClient();

        if (backend.isPresent() && backend.get().equals("mongo")) {
            dao = daoFactory.createMongoUserDAO();
            conversationDao = daoFactory.createSQLConversationDAO();
        } else {
            dao = daoFactory.createSQLUserDAO();
            conversationDao = daoFactory.createSQLConversationDAO();
        }

        var grpcServerName = Optional.ofNullable(System.getenv("GRPC_SERVER"));
        var grpcServerPort = Optional.ofNullable(System.getenv("GRPC_SERVER_PORT"));

        channel = ManagedChannelBuilder
                .forAddress(grpcServerName.orElse("backend-grpc"), Integer.parseInt(
                        grpcServerPort.orElse("50051")))
                .usePlaintext()
                .build();

        blockingStub = GrpcServiceGrpc.newBlockingStub(channel);

        // grpcClient = new GrpcServiceClient();
    }

    public static AppLogicImpl getInstance() {
        return instance;
    }

    public Optional<User> getUserByEmail(String userId) {
        return dao.getUserByEmail(userId);
    }

    public Optional<User> getUserById(String userId) {
        return dao.getUserById(userId);
    }

    public IUserDAO getUserDAO() {
        return dao;
    }

    public void saveConversation(String userId, String dialogueId, String dname, String status, String dialogue) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setDialogueId(dialogueId);
        conv.setDname(dname);
        conv.setStatus(status);
        conv.setDialogue(dialogue);
        // createdAt se pone por defecto en la base de datos
        conversationDao.save(conv);
    }

    public boolean ping(int v) {
        logger.info("Enviando ping al servicio gRPC con valor: " + v);
        try {
            PingRequest request = PingRequest.newBuilder().setV(v).build();
            var response = blockingStub.ping(request);
            return response.getV() == v;
        } catch (Exception e) {
            logger.severe("Error durante el ping al servicio gRPC: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envía un prompt al servicio gRPC usando sendPrompt() y devuelve el token como
     * respuesta
     */
    public String fetchPromptResponse(String promptText) {
        logger.info("Enviando prompt al servicio gRPC: " + promptText);
        try {
            PromptRequest request = PromptRequest.newBuilder()
                    .setPrompt(promptText)
                    .build();
            PromptResponse response = blockingStub.sendPrompt(request);
            this.lastPromptResponse = response.getToken(); // Guardamos respuesta
            return response.getToken();
        } catch (Exception e) {
            logger.severe("Error al procesar el prompt en el servicio gRPC: " + e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio gRPC", e);
        }
    }

    public String fetchPromptResponseSync(String promptText) {
        logger.info("Enviando prompt al servicio gRPC (sincrónico): " + promptText);
        try {
            // 1. Enviar prompt → obtener token
            PromptRequest request = PromptRequest.newBuilder()
                    .setPrompt(promptText)
                    .build();

            PromptResponse response = blockingStub.sendPrompt(request);
            String token = response.getToken();
            logger.info("Token recibido del servicio gRPC: " + token);

            // 2. Polling → esperar hasta obtener respuesta completa
            while (true) {
                ResponseRequest responseRequest = ResponseRequest.newBuilder()
                        .setToken(token)
                        .build();

                ResponseResponse responseResponse = blockingStub.getResponse(responseRequest);

                if (responseResponse.getStatus().equals("completed")) {
                    logger.info("Respuesta obtenida del modelo: " + responseResponse.getAnswer());
                    return responseResponse.getAnswer(); // Devuelve el texto completo generado
                } else {
                    logger.info("Respuesta aún no disponible, esperando...");
                    Thread.sleep(2000); // Esperar 2 segundos antes de reintentar
                }
            }
        } catch (Exception e) {
            logger.severe("Error al procesar el prompt en el servicio gRPC: " + e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio gRPC", e);
        }
    }

    public String getLastResponse_grpc() {
        return grpcClient.getLastResponse();
    }

    public List<Conversation> getUserConversations(String userId) {
        // Devuelve la lista completa de objetos Conversation
        return conversationDao.findByUserId(userId);
    }

    public boolean deleteUserConversation(String userId, String dialogueId) {
        return conversationDao.deleteByUserIdAndDialogueId(userId, dialogueId);
    }

    public Optional<User> checkLogin(String email, String pass) {
        Optional<User> u = dao.getUserByEmail(email);

        if (u.isPresent()) {
            String hashed_pass = UserUtils.md5pass(pass);
            if (hashed_pass.equals(u.get().getPassword_hash())) {
                // Incrementar visitas
                dao.incrementVisits(u.get().getId());
                // Actualizar el objeto User con el nuevo número de visitas
                u.get().setVisits(u.get().getVisits() + 1);
                return u;
            }
        }
        return Optional.empty();
    }

    // Devuelve el número total de usuarios registrados
    public int getTotalUsers() {
        return dao.countUsers();
    }

    // Devuelve el número total de conversaciones en la base de datos
    public int getTotalConversations() {
        return conversationDao.countConversations();
    }

    // Devuelve el número de visitas del usuario
    public int getUserVisits(String userId) {
        return dao.getUserVisits(userId);
    }

    // Devuelve el número de conversaciones del usuario
    public int getUserConversationsCount(String userId) {
        return conversationDao.countConversationsByUser(userId);
    }
}