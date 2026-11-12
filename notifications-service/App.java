import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(
            new InetSocketAddress(8082), 0);

        server.createContext("/notifications", exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String message = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

            System.out.println("NEW NOTIFICATION: " + message);

            byte[] response =
                "{\"status\":\"notification received\"}"
                .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        System.out.println("Notifications Service started on port 8082");
        server.start();
    }
}
