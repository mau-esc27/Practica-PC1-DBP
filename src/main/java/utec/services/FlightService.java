package utec.services;

import org.json.JSONObject;
import utec.dtos.FlightDTO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FlightService {
    private final Map<String, FlightDTO> flights = new ConcurrentHashMap<>();

    public FlightDTO createFlight(JSONObject json) {
        FlightDTO flight = new FlightDTO();
        flight.id = UUID.randomUUID().toString();
        flight.airlineName = json.getString("airlineName");
        flight.flightNumber = json.getString("flightNumber");
        flight.estDepartureTime = json.get("estDepartureTime").toString();
        flight.estArrivalTime = json.get("estArrivalTime").toString();
        flight.availableSeats = json.getInt("availableSeats");

        flights.put(flight.id, flight);
        return flight;
    }

    public boolean existsFlightNumber(String flightNumber) {
        return flights.values().stream().anyMatch(f -> f.flightNumber.equals(flightNumber));
    }

    public List<FlightDTO> searchFlights(String flightNumber) {
        if (flightNumber == null || flightNumber.isEmpty()) {
            return new ArrayList<>(flights.values());
        }
        return flights.values().stream()
                .filter(f -> f.flightNumber.equalsIgnoreCase(flightNumber))
                .toList();
    }

    public FlightDTO getFlightById(String id) {
        return flights.get(id);
    }

    public void clear() {
        flights.clear();
    }
}
