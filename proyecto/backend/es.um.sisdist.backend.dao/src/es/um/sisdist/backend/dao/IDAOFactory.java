/**
 *
 */
package es.um.sisdist.backend.dao;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.backend.dao.conversation.IConversationDAO;
import es.um.sisdist.backend.dao.conversation.IConversationDAO;
import es.um.sisdist.backend.dao.user.IUserDAO;

/**
 * @author dsevilla
 *
 */
public interface IDAOFactory
{
    public IUserDAO createSQLUserDAO();
    public IConversationDAO createSQLConversationDAO();
    public IConversationDAO createMongoConversationDAO();
    public IUserDAO createMongoUserDAO();
}
