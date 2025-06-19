/**
 *
 */
package es.um.sisdist.backend.dao.user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.utils.Lazy;

/**
 * @author dsevilla
 *
 */
public class SQLUserDAO implements IUserDAO {
    Supplier<Connection> conn;

    public SQLUserDAO() {
        conn = Lazy.lazily(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();

                // Si el nombre del host se pasa por environment, se usa aquí.
                // Si no, se usa localhost. Esto permite configurarlo de forma
                // sencilla para cuando se ejecute en el contenedor, y a la vez
                // se pueden hacer pruebas locales
                String sqlServerName = Optional.ofNullable(System.getenv("SQL_SERVER")).orElse("localhost");
                String dbName = Optional.ofNullable(System.getenv("DB_NAME")).orElse("ssdd");
                return DriverManager.getConnection(
                        "jdbc:mysql://" + sqlServerName + "/" + dbName + "?user=root&password=root");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                return null;
            }
        });
    }

    @Override
    public Optional<User> getUserById(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<User> getUserByEmail(String id) {
        PreparedStatement stm;
        try {
            stm = conn.get().prepareStatement("SELECT * from users WHERE email = ?");
            stm.setString(1, id);
            ResultSet result = stm.executeQuery();
            if (result.next())
                return createUser(result);
        } catch (SQLException e) {
            // Fallthrough
        }
        return Optional.empty();
    }

    private Optional<User> createUser(ResultSet result) {
        try {
            return Optional.of(new User(result.getString(1), // id
                    result.getString(2), // email
                    result.getString(3), // pwhash
                    result.getString(4), // name
                    result.getString(5), // token
                    result.getInt(6))); // visits
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    @Override
    public void save(User user) {
        PreparedStatement stm = null;
        try {
            stm = conn.get().prepareStatement(
                    "INSERT INTO users (id, email, password_hash, name, token, visits) VALUES (?, ?, ?, ?, ?, ?)");
            stm.setString(1, user.getId());
            stm.setString(2, user.getEmail());
            stm.setString(3, user.getPassword_hash());
            stm.setString(4, user.getName());
            stm.setString(5, user.getToken());
            stm.setInt(6, user.getVisits());
            stm.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e); // Lanza como RuntimeException
        } finally {
            if (stm != null) {
                try {
                    stm.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public void incrementVisits(String userId) {
        try (PreparedStatement stm = conn.get().prepareStatement(
                "UPDATE users SET visits = visits + 1 WHERE id = ?")) {
            stm.setString(1, userId);
            stm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Cuenta usuarios
    @Override
    public int countUsers() {
        try (PreparedStatement stm = conn.get().prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = stm.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Obtiene visitas de un usuario
    @Override
    public int getUserVisits(String userId) {
        try (PreparedStatement stm = conn.get().prepareStatement("SELECT visits FROM users WHERE id = ?")) {
            stm.setString(1, userId);
            ResultSet rs = stm.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
