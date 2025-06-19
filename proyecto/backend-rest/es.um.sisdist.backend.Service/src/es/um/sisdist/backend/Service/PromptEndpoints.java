
package es.um.sisdist.backend.Service;

//import es.um.sisdist.backend.Service.PromptGrpcClient;
//import java.util.concurrent.ConcurrentHashMap;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.google.gson.JsonObject;

@Path("/prompt")
public class PromptEndpoints {

    // private final PromptGrpcClient grpcClient = new PromptGrpcClient();
    // private static final ConcurrentHashMap<String, String> responses = new
    // ConcurrentHashMap<>();
    private AppLogicImpl impl = AppLogicImpl.getInstance();

    /*
     * @POST
     * 
     * @Consumes(MediaType.APPLICATION_JSON)
     * 
     * @Produces(MediaType.APPLICATION_JSON)
     * public Response handlePrompt(PromptRequest request) {
     * //String token = UUID.randomUUID().toString();
     * String token = impl.fetchPromptResponse(request.getPrompt());
     * responses.put(token, "Respuesta de prueba");
     * return Response.accepted()
     * .header("Location", "/response/" + token)
     * .build();
     * }
     */

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handlePrompt(PromptRequest request) {
        try {
            // Procesar el prompt y obtener la respuesta
            String response = impl.fetchPromptResponseSync(request.getPrompt());

            String userId = request.getUserId();
            String dialogueId = java.util.UUID.randomUUID().toString();
            String dname = request.getPrompt();
            String status = "READY";

            // Guardar prompt y respuesta como JSON en el campo dialogue
            JsonObject dialogueJson = new JsonObject();
            dialogueJson.addProperty("prompt", request.getPrompt());
            dialogueJson.addProperty("answer", response);

            String dialogue = dialogueJson.toString();

            impl.saveConversation(userId, dialogueId, dname, status, dialogue);

            return Response.ok(new PromptResponse(response)).build();
        } catch (Exception e) {
            return Response.serverError().entity("Error al procesar el prompt: " + e.getMessage()).build();
        }
    }

    /*
     * @GET
     * 
     * @Path("/response/{token}")
     * 
     * @Produces(MediaType.APPLICATION_JSON)
     * public Response getResponse(@PathParam("token") String token) {
     * String response = responses.get(token);
     * if (response == null) {
     * return Response.status(Response.Status.NO_CONTENT).build(); // Aún procesando
     * }
     * responses.remove(token); // Eliminar la respuesta después de recuperarla
     * return Response.ok(new PromptResponse(response)).build();
     * }
     */

    public static class PromptRequest {
        private String prompt;
        private String userId; // Añadido para identificar al usuario

        public PromptRequest() {
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    public static class PromptResponse {
        private String answer;

        public PromptResponse() {
        } // JAX-RS necesita constructor vacío

        public PromptResponse(String answer) {
            this.answer = answer;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

}