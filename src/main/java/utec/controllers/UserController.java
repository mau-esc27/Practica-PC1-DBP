package utec.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import utec.dto.*;
import utec.models.User;
import utec.repository.Database;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UserController {

    // POST /users/register
    public static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(body);

            RegisterUserDTO dto = new RegisterUserDTO();
            dto.firstName = json.getString("firstName");
            dto.lastName = json.getString("lastName");
            dto.email = json.getString("email");
            dto.password = json.getString("password");

            if (Database.users.values().stream().anyMatch(us -> us.email.equals(dto.email))) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            User u = new User();
            u.firstName = dto.firstName;
            u.lastName = dto.lastName;
            u.email = dto.email;
            u.passwordHash = Integer.toHexString(dto.password.hashCode());

            Database.users.put(u.id, u);

            NewIdDTO respDTO = new NewIdDTO(u.id);
            JSONObject resp = new JSONObject(respDTO);

            sendJSON(exchange, resp);
        }
    }

    // GET /users/{id}
    public static class GetUserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath(); // /users/{id}
            String[] parts = path.split("/");
            if (parts.length < 3) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String id = parts[2];
            User u = Database.users.get(id);
            if (u == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            JSONObject resp = new JSONObject();
            resp.put("id", u.id);
            resp.put("firstName", u.firstName);
            resp.put("lastName", u.lastName);
            resp.put("email", u.email);

            sendJSON(exchange, resp);
        }
    }

    private static void sendJSON(HttpExchange exchange, JSONObject resp) throws IOException {
        byte[] out = resp.toString().getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }
}
