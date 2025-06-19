package es.um.sisdist.backend.Service;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.google.gson.JsonObject;

@Path("stats")
public class StatsEndpoints {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats(@QueryParam("userId") String userId) {
        int totalUsers = AppLogicImpl.getInstance().getTotalUsers();
        int totalConversations = AppLogicImpl.getInstance().getTotalConversations();
        int userVisits = AppLogicImpl.getInstance().getUserVisits(userId);
        int userConversations = AppLogicImpl.getInstance().getUserConversationsCount(userId);

        JsonObject obj = new JsonObject();
        obj.addProperty("total_users", totalUsers);
        obj.addProperty("total_conversations", totalConversations);
        obj.addProperty("user_visits", userVisits);
        obj.addProperty("user_conversations", userConversations);

        return Response.ok(obj.toString()).build();
    }
}