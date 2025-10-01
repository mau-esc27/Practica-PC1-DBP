package utec;

import com.sun.net.httpserver.HttpServer;
import utec.controllers.*;

import java.net.InetSocketAddress;

public class Application {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        // Usuarios
        server.createContext("/users/register", new UserController.RegisterHandler());
        server.createContext("/users", new UserController.GetUserHandler());

        // Auth
        server.createContext("/auth/login", new AuthController.LoginHandler());

        // Flights
        server.createContext("/flights/create", new FlightController.CreateFlightHandler());
        server.createContext("/flights/search", new FlightController.SearchFlightsHandler());
        server.createContext("/flights/book", new FlightController.BookFlightHandler());
        server.createContext("/flights/book/", new FlightController.GetBookingHandler());

        // Cleanup
        server.createContext("/cleanup", new CleanupController());

        server.setExecutor(null);
        System.out.println("🚀 Servidor en http://localhost:8080");
        server.start();
    }
}
