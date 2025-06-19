package testclient;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import org.json.JSONObject;
import org.json.JSONArray;

public class TestClient {
    public static void main(String[] args) {
        Client client = ClientBuilder.newClient();

        // 1. Registro de usuario (añade el campo "name")
        JSONObject user = new JSONObject();
        user.put("email", "test@client.com");
        user.put("password", "admin123");
        user.put("name", "test@client.com"); // Usa el email como name/id

        Response reg = client.target("http://localhost:8080/Service/Registro")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(user.toString()));
        String regResponseStr = reg.readEntity(String.class);
        System.out.println("Registro: " + reg.getStatus() + " " + regResponseStr);
        String userId = "test@client.com"; // valor por defecto
        try {
            JSONObject regJson = new JSONObject(regResponseStr);
            if (regJson.has("id"))
                userId = regJson.get("id").toString();
        } catch (Exception e) {
            System.out.println("No se pudo obtener userId del registro, usando 'test@client.com'");
        }

        // 2. Crear conversación (prompt)
        JSONObject prompt = new JSONObject();
        prompt.put("userId", userId);
        prompt.put("prompt", "¿Cuál es una ciudad de España?");
        Response conv = client.target("http://localhost:8080/Service/prompt")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(prompt.toString()));
        System.out.println("Prompt: " + conv.getStatus() + " " + conv.readEntity(String.class));

        // 3. Listar conversaciones
        Response list = client.target("http://localhost:8080/Service/u/" + userId + "/dialogue")
                .request(MediaType.APPLICATION_JSON)
                .get();
        String convListStr = list.readEntity(String.class);
        System.out.println("Conversaciones: " + convListStr);

        // 4. Eliminar conversación (usa un dialogueId real obtenido antes)
        String dialogueId = null;
        try {
            JSONArray arr = new JSONArray(convListStr);
            if (arr.length() > 0) {
                dialogueId = arr.getJSONObject(0).getString("dialogueId");
            }
        } catch (Exception e) {
            System.out.println("No se pudo obtener dialogueId");
        }
        if (dialogueId != null) {
            Response del = client.target("http://localhost:8080/Service/u/" + userId + "/dialogue/" + dialogueId)
                    .request().delete();
            System.out.println("Eliminar: " + del.getStatus() + " " + del.readEntity(String.class));
        } else {
            System.out.println("No hay conversaciones para borrar.");
        }
    }
}
