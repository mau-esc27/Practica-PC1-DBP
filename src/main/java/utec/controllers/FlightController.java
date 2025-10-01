package utec.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import utec.dto.*;
import utec.models.*;
import utec.repository.Database;
import utec.apitester.utils.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class FlightController {

    // POST /flights/create
    public static class CreateFlightHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(body);

            NewFlightRequestDTO dto = new NewFlightRequestDTO();
            dto.airlineName = json.getString("airlineName");
            dto.flightNumber = json.getString("flightNumber");
            dto.estDepartureTime = json.getString("estDepartureTime");
            dto.estArrivalTime = json.getString("estArrivalTime");
            dto.availableSeats = json.getInt("availableSeats");

            Flight f = new Flight();
            f.airlineName = dto.airlineName;
            f.flightNumber = dto.flightNumber;
            f.estDepartureTime = Instant.parse(dto.estDepartureTime);
            f.estArrivalTime = Instant.parse(dto.estArrivalTime);
            f.availableSeats = dto.availableSeats;

            if (Database.flights.values().stream().anyMatch(fl -> fl.flightNumber.equals(f.flightNumber))) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            Database.flights.put(f.id, f);

            NewIdDTO respDTO = new NewIdDTO(f.id);
            JSONObject resp = new JSONObject(respDTO);
            sendJSON(exchange, resp);
        }
    }

    // GET /flights/search
    public static class SearchFlightsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // JWT
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            String userId = JwtUtil.validateToken(auth.substring(7));
            if (userId == null) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String flightNumber;
            if (query != null && query.contains("flightNumber=")) {
                flightNumber = query.split("=")[1];
            } else {
                flightNumber = null;
            }

            List<Flight> result = Database.flights.values().stream()
                    .filter(f -> flightNumber == null || f.flightNumber.contains(flightNumber))
                    .collect(Collectors.toList());

            JSONArray arr = new JSONArray();
            for (Flight f : result) {
                FlightSearchResponseDTO dto = new FlightSearchResponseDTO();
                dto.id = f.id;
                dto.airlineName = f.airlineName;
                dto.flightNumber = f.flightNumber;
                dto.estDepartureTime = f.estDepartureTime.toString();
                dto.estArrivalTime = f.estArrivalTime.toString();
                dto.availableSeats = f.availableSeats - f.seatsBooked;

                arr.put(new JSONObject(dto));
            }

            sendJSON(exchange, arr);
        }
    }

    // POST /flights/book
    public static class BookFlightHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            String userId = JwtUtil.validateToken(auth.substring(7));
            if (userId == null) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            User user = Database.users.get(userId);

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(body);

            FlightBookRequestDTO dto = new FlightBookRequestDTO();
            dto.flightId = json.getString("flightId");

            Flight flight = Database.flights.get(dto.flightId);
            if (flight == null || flight.seatsBooked >= flight.availableSeats) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            flight.seatsBooked++;

            Booking b = new Booking();
            b.flightId = flight.id;
            b.flightNumber = flight.flightNumber;
            b.customerId = user.id;
            b.customerFirstName = user.firstName;
            b.customerLastName = user.lastName;

            Database.bookings.put(b.id, b);

            // Generar archivo confirmación
            String content = String.format(
                    "Hello %s %s,\n\nYour booking was successful!\n\nThe booking is for flight %s with departure date of %s and arrival date of %s.\n\nThe booking was registered at %s.\n\nBon Voyage!\nFly Away Travel",
                    user.firstName, user.lastName,
                    flight.flightNumber, flight.estDepartureTime, flight.estArrivalTime, b.bookingDate
            );
            Files.writeString(Path.of("flight_booking_email_" + b.id + ".txt"), content);

            NewIdDTO respDTO = new NewIdDTO(b.id);
            JSONObject resp = new JSONObject(respDTO);
            sendJSON(exchange, resp);
        }
    }

    // GET /flights/book/{id}
    public static class GetBookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String id = parts[3];
            Booking b = Database.bookings.get(id);
            if (b == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            BookingResponseDTO dto = new BookingResponseDTO();
            dto.id = b.id;
            dto.bookingDate = b.bookingDate.toString();
            dto.flightId = b.flightId;
            dto.flightNumber = b.flightNumber;
            dto.customerId = b.customerId;
            dto.customerFirstName = b.customerFirstName;
            dto.customerLastName = b.customerLastName;

            JSONObject resp = new JSONObject(dto);
            sendJSON(exchange, resp);
        }
    }

    private static void sendJSON(HttpExchange exchange, Object obj) throws IOException {
        byte[] out = obj.toString().getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }
}
