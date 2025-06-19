package es.um.sisdist.backend.Service;

import java.sql.SQLException;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.models.RegisterUser;
import es.um.sisdist.models.UserDTO;
import es.um.sisdist.models.UserDTOUtils;
import es.um.sisdist.backend.dao.models.User;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/Registro")
public class Registro {
    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(RegisterUser request) {
        // Comprobar si el usuario ya existe por email
        if (impl.getUserByEmail(request.getEmail()).isPresent()) {
            return Response.status(Status.CONFLICT)
                    .entity("El usuario ya existe").build();
        }

        // Crear el usuario real (modelo User)
        User user = new User();
        user.setId(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword_hash(es.um.sisdist.backend.dao.models.utils.UserUtils.md5pass(request.getPassword()));
        user.setName(request.getName());
        user.setToken(""); // O genera un token si lo usas
        user.setVisits(0);

        // Guardar el usuario en la base de datos
        try {
            impl.getUserDAO().save(user);
        } catch (Exception e) { // <-- Cambia SQLException por Exception
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al guardar el usuario: " + e.getMessage())
                    .build();
        }

        // Devolver el DTO del usuario creado
        return Response.ok(UserDTOUtils.toDTO(user)).build();
    }
}