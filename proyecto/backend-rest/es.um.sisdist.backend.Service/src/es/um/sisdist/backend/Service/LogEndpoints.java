package es.um.sisdist.backend.Service;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.backend.dao.models.Conversation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/u/{userId}/dialogue")
public class LogEndpoints {

    // Obtener todas las conversaciones de un usuario
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserDialogues(@PathParam("userId") String userId) {
        try {
            List<Conversation> conversations = AppLogicImpl.getInstance().getUserConversations(userId);
            return Response.ok(conversations).build();
        } catch (Exception e) {
            return Response.serverError().entity("Error al obtener conversaciones: " + e.getMessage()).build();
        }
    }

    // Eliminar una conversación concreta
    @DELETE
    @Path("/{dialogueId}")
    public Response deleteDialogue(@PathParam("userId") String userId, @PathParam("dialogueId") String dialogueId) {
        try {
            boolean deleted = AppLogicImpl.getInstance().deleteUserConversation(userId, dialogueId);
            if (deleted)
                return Response.ok().entity("Conversación eliminada").build();
            else
                return Response.status(404).entity("No encontrada").build();
        } catch (Exception e) {
            return Response.serverError().entity("Error al eliminar: " + e.getMessage()).build();
        }
    }
}