package utec.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import utec.dto.*;
import utec.models.User;
import utec.repository.Database;
import utec.apitester.utils.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AuthController {

    // POST /auth/login
    public static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(body);

            LoginDTO dto = new LoginDTO();
            dto.email = json.getString("email");
            dto.password = json.getString("password");

            User u = Database.users.values().stream()
                    .filter(us -> us.email.equals(dto.email))
                    .findFirst().orElse(null);

            if (u == null || !u.passwordHash.equals(Integer.toHexString(dto.password.hashCode()))) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            AuthTokenDTO tokenDTO = new AuthTokenDTO(JwtUtil.generateToken(u.id));
            JSONObject resp = new JSONObject(tokenDTO);

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
