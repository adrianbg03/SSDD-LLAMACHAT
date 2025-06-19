package es.um.sisdist.backend.dao.conversation;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import es.um.sisdist.backend.dao.models.Conversation;

public class MongoConversationDAO implements IConversationDAO {
    private final MongoCollection<Document> conversations;

    public MongoConversationDAO() {
        String mongoUri = Optional.ofNullable(System.getenv("MONGO_URI")).orElse("mongodb://localhost:27017");
        String dbName = Optional.ofNullable(System.getenv("MONGO_DB")).orElse("ssdd");

        MongoClient mongoClient = MongoClients.create(mongoUri);
        MongoDatabase database = mongoClient.getDatabase(dbName);
        this.conversations = database.getCollection("conversations");
    }

    @Override
    public void save(Conversation conversation) {
        Document doc = new Document()
                .append("dialogue_id", conversation.getDialogueId())
                .append("user_id", conversation.getUserId())
                .append("dname", conversation.getDname())
                .append("status", conversation.getStatus())
                .append("dialogue", conversation.getDialogue()) // Puede ser String o JSON
                .append("created_at", conversation.getCreatedAt());

        conversations.insertOne(doc);
    }

    @Override
    public List<Conversation> findByUserId(String userId) {
        List<Conversation> result = new ArrayList<>();
        conversations.find(Filters.eq("user_id", userId)).forEach(doc -> result.add(toConversation(doc)));
        return result;
    }

    @Override
    public Conversation findById(int id) {
        throw new UnsupportedOperationException(
                "findById(int id) no implementado para MongoDB. Usa findByDialogueId en su lugar.");
    }

    // Método específico para MongoDB (alternativa a findById)
    public Conversation findByDialogueId(String dialogueId) {
        Document doc = conversations.find(Filters.eq("dialogue_id", dialogueId)).first();
        return doc != null ? toConversation(doc) : null;
    }

    private Conversation toConversation(Document doc) {
        Conversation c = new Conversation();
        c.setDialogueId(doc.getString("dialogue_id"));
        c.setUserId(doc.getString("user_id"));
        c.setDname(doc.getString("dname"));
        c.setStatus(doc.getString("status"));
        c.setDialogue(doc.get("dialogue").toString());
        c.setCreatedAt(doc.get("created_at", Timestamp.class)); // Puede necesitar conversión
        return c;
    }

    @Override
    public boolean deleteByUserIdAndDialogueId(String userId, String dialogueId) {
        throw new UnsupportedOperationException("Not implemented for MongoConversationDAO");
    }

    @Override
    public int countConversationsByUser(String userId) {
        // Implementación real si usas Mongo, si no, devuelve 0 para compilar
        return 0;
    }

    @Override
    public int countConversations() {
        return (int) conversations.countDocuments();
    }
}
