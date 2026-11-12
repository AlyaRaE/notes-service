import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    static String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    static String DB_PORT = System.getenv().getOrDefault("DB_PORT", "5432");
    static String DB_NAME = System.getenv().getOrDefault("DB_NAME", "app");
    static String DB_USER = System.getenv().getOrDefault("DB_USER", "postgres");
    static String DB_PASS = System.getenv().getOrDefault("DB_PASS", "1234");

    static String URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    static String NOTIFICATIONS_URL = System.getenv().getOrDefault(
        "NOTIFICATIONS_URL", "http://localhost:8082/notifications"
    );

    public static void main(String[] args) throws Exception {
        initDB();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/notes", App::handleNotes);

        System.out.println("Notes Service started on port 8080");
        server.start();
    }

    static void initDB() throws Exception {
        for (int attempt = 1; attempt <= 30; attempt++) {
            try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS notes (" +
                    "id SERIAL PRIMARY KEY, text VARCHAR(255) NOT NULL)"
                );
                System.out.println("Table 'notes' is ready.");
                return;
            } catch (SQLException e) {
                System.out.println("Waiting for database, attempt " + attempt);
                Thread.sleep(1000);
            }
        }
        throw new RuntimeException("Could not connect to database");
    }

    static void handleNotes(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/notes")) {
                if (method.equals("GET")) {
                    sendJson(exchange, 200, getAllNotes());
                    return;
                }

                if (method.equals("POST")) {
                    String body = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                    );
                    String text = extractText(body);

                    if (text.isBlank()) {
                        sendJson(exchange, 400, "{\"error\":\"text cannot be empty\"}");
                        return;
                    }

                    int id = addNote(text);
                    sendNotification();
                    sendJson(exchange, 201,
                        "{\"id\":" + id + ",\"text\":\"" + escapeJson(text) + "\"}");
                    return;
                }

                sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }

            if (path.startsWith("/notes/")) {
                int id;
                try {
                    id = Integer.parseInt(path.substring("/notes/".length()));
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"invalid note id\"}");
                    return;
                }

                if (method.equals("PUT")) {
                    String body = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                    );
                    String text = extractText(body);

                    if (!updateNote(id, text)) {
                        sendJson(exchange, 404, "{\"error\":\"note not found\"}");
                        return;
                    }

                    sendJson(exchange, 200,
                        "{\"id\":" + id + ",\"text\":\"" + escapeJson(text) + "\"}");
                    return;
                }

                if (method.equals("DELETE")) {
                    if (!deleteNote(id)) {
                        sendJson(exchange, 404, "{\"error\":\"note not found\"}");
                        return;
                    }

                    sendJson(exchange, 200, "{\"status\":\"deleted\"}");
                    return;
                }

                sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"not found\"}");

        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"invalid request\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"internal server error\"}");
        }
    }

    static String getAllNotes() throws SQLException {
        StringBuilder sb = new StringBuilder("[");
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, text FROM notes ORDER BY id")) {

            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"id\":").append(rs.getInt("id"))
                  .append(",\"text\":\"")
                  .append(escapeJson(rs.getString("text"))).append("\"}");
                first = false;
            }
        }
        return sb.append("]").toString();
    }

    static int addNote(String text) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO notes(text) VALUES (?) RETURNING id")) {
            pstmt.setString(1, text);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    static boolean updateNote(int id, String text) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE notes SET text = ? WHERE id = ?")) {
            pstmt.setString(1, text);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    static boolean deleteNote(int id) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(
                 "DELETE FROM notes WHERE id = ?")) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    static void sendNotification() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NOTIFICATIONS_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"message\":\"A new note was created\"}"
                ))
                .build();

            HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Notification service response: " + response.statusCode());

        } catch (Exception e) {
            System.err.println("Could not send notification: " + e.getMessage());
        }
    }

    static String extractText(String body) {
        Matcher matcher = Pattern.compile(
            "\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        ).matcher(body);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing text field");
        }

        return matcher.group(1)
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    static void sendJson(HttpExchange exchange, int code, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

