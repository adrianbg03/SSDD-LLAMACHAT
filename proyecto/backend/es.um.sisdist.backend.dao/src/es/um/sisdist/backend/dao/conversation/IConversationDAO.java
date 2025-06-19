package es.um.sisdist.backend.dao.conversation;

import es.um.sisdist.backend.dao.models.Conversation;
import java.util.List;

public interface IConversationDAO {
    void save(Conversation conversation);

    List<Conversation> findByUserId(String userId);

    Conversation findById(int id);

    boolean deleteByUserIdAndDialogueId(String userId, String dialogueId);

    int countConversations();

    int countConversationsByUser(String userId);

}