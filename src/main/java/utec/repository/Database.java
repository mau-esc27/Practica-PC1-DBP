package utec.repository;

import utec.models.*;

import java.util.*;

public class Database {
    public static final Map<String, User> users = new HashMap<>();
    public static final Map<String, Flight> flights = new HashMap<>();
    public static final Map<String, Booking> bookings = new HashMap<>();

    public static void clear() {
        users.clear();
        flights.clear();
        bookings.clear();
    }
}
