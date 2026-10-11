import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class App {

    static String DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
    static String DB_PORT = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "5432";
    static String DB_NAME = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "app";
    static String DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
    static String DB_PASS = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "1234";

    static String URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    public static void main(String[] args) throws IOException {

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) { }

        initDB();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/notes", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            try {
                if (path.equals("/notes")) {
                    if (method.equals("GET")) {
                        sendJson(exchange, 200, getAllNotes());
                    } else if (method.equals("POST")) {
                        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        String text = extractText(body);
                        int id = addNote(text);
                        sendJson(exchange, 201, "{\"id\":" + id + ",\"text\":\"" + text + "\"}");
                    }
                    return;
                }

                if (path.startsWith("/notes/")) {
                    int id = Integer.parseInt(path.substring("/notes/".length()));
                    if (method.equals("PUT")) {
                        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        String text = extractText(body);
                        updateNote(id, text);
                        sendJson(exchange, 200, "{\"id\":" + id + ",\"text\":\"" + text + "\"}");
                    } else if (method.equals("DELETE")) {
                        deleteNote(id);
                        sendJson(exchange, 200, "{\"status\":\"deleted\"}");
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }

            sendJson(exchange, 404, "{\"error\":\"not found\"}");
        });

        server.setExecutor(null);
        System.out.println("Server started on port 8080...");
        server.start();
    }

    static void initDB() {
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS notes (" +
                    "id SERIAL PRIMARY KEY," +
                    "text VARCHAR(255) NOT NULL" +
                    ");";
            stmt.execute(sql);
            System.out.println("Table 'notes' is ready.");
        } catch (SQLException e) {
            System.err.println("DB init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static String getAllNotes() {
        StringBuilder sb = new StringBuilder("[");
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, text FROM notes ORDER BY id")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"id\":").append(rs.getInt("id")).append(",\"text\":\"").append(rs.getString("text")).append("\"}");
                first = false;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        sb.append("]");
        return sb.toString();
    }

    static int addNote(String text) {
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO notes (text) VALUES (?) RETURNING id")) {
            pstmt.setString(1, text);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    static void updateNote(int id, String text) {
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE notes SET text = ? WHERE id = ?")) {
            pstmt.setString(1, text);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    static void deleteNote(int id) {
        try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }


    static void sendJson(com.sun.net.httpserver.HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static String extractText(String body) {
        int idx = body.indexOf("\"text\"");
        if (idx == -1) return "";
        int start = body.indexOf("\"", idx + 6);
        int end = body.indexOf("\"", start + 1);
        return body.substring(start + 1, end);
    }
}
