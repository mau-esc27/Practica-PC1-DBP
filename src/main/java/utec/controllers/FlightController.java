package utec.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import utec.dtos.FlightDTO;
import utec.services.ServiceRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class FlightController {

    public static class CreateFlightHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body;
            try (Scanner scanner = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }

            if (body.isBlank()) {
                sendResponse(exchange, 400, new JSONObject().put("error", "Missing body"));
                return;
            }

            JSONObject json;
            try {
                json = new JSONObject(body);
            } catch (Exception e) {
                sendResponse(exchange, 400, new JSONObject().put("error", "Invalid JSON"));
                return;
            }

            if (!json.has("airlineName") || !json.has("flightNumber") ||
                    !json.has("estDepartureTime") || !json.has("estArrivalTime") ||
                    !json.has("availableSeats")) {
                sendResponse(exchange, 400, new JSONObject().put("error", "Missing fields"));
                return;
            }

            String flightNumber = json.getString("flightNumber");
            int availableSeats = json.getInt("availableSeats");

            // Validación número de vuelo
            if (!flightNumber.matches("^[A-Z]{2,3}[0-9]{3}$")) {
                sendResponse(exchange, 400, new JSONObject().put("error", "Invalid flight number format"));
                return;
            }

            // Validación seats > 0
            if (availableSeats <= 0) {
                sendResponse(exchange, 400, new JSONObject().put("error", "Available seats must be greater than zero"));
                return;
            }

            // Único
            if (ServiceRegistry.FLIGHT.existsFlightNumber(flightNumber)) {
                sendResponse(exchange, 400, new JSONObject().put("error", "Flight already exists"));
                return;
            }

            FlightDTO flight = ServiceRegistry.FLIGHT.createFlight(json);
            sendResponse(exchange, 201, new JSONObject().put("id", flight.id));
        }
    }

    public static class SearchFlightsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String flightNumber = null;
            if (query != null && query.startsWith("flightNumber=")) {
                flightNumber = query.split("=")[1];
            }

            List<FlightDTO> results = ServiceRegistry.FLIGHT.searchFlights(flightNumber);
            JSONObject response = new JSONObject();
            response.put("results", results.stream().map(f ->
                    new JSONObject()
                            .put("id", f.id)
                            .put("airlineName", f.airlineName)
                            .put("flightNumber", f.flightNumber)
                            .put("estDepartureTime", f.estDepartureTime)
                            .put("estArrivalTime", f.estArrivalTime)
                            .put("availableSeats", f.availableSeats)
            ).toArray());
            sendResponse(exchange, 200, response);
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, JSONObject response) throws IOException {
        byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
