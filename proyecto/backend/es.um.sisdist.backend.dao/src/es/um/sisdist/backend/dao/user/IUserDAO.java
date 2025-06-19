package es.um.sisdist.backend.dao.user;

import java.sql.SQLException;
import java.util.Optional;

import es.um.sisdist.backend.dao.models.User;

public interface IUserDAO {
    public Optional<User> getUserById(String id);

    public Optional<User> getUserByEmail(String id);

    void save(User user) throws SQLException;

    void incrementVisits(String userId);

    int countUsers();

    int getUserVisits(String userId);
}
