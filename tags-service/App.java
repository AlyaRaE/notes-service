import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    static String url = "jdbc:postgresql://" +
        System.getenv().getOrDefault("DB_HOST", "localhost") +
        ":5432/" +
        System.getenv().getOrDefault("DB_NAME", "app");

    static String user = System.getenv().getOrDefault("DB_USER", "postgres");
    static String pass = System.getenv().getOrDefault("DB_PASS", "1234");

    public static void main(String[] args) throws Exception {
        initDB();

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8081), 0
        );

        server.createContext("/tags", App::handleTags);

        System.out.println("Tags Service started on port 8081");
        server.start();
    }

    static void initDB() throws Exception {
        for (int i = 1; i <= 30; i++) {
            try (Connection c = DriverManager.getConnection(url, user, pass);
                 Statement s = c.createStatement()) {
                s.execute(
                    "CREATE TABLE IF NOT EXISTS tags (" +
                    "id SERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL)"
                );
                System.out.println("Table 'tags' is ready.");
                return;
            } catch (SQLException e) {
                System.out.println("Waiting for database, attempt " + i);
                Thread.sleep(1000);
            }
        }
        throw new RuntimeException("Could not connect to database");
    }

    static void handleTags(HttpExchange exchange) throws java.io.IOException {
        try {
            String method = exchange.getRequestMethod();

            if (method.equals("POST")) {
                String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
                );

                Matcher matcher = Pattern.compile(
                    "\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
                ).matcher(body);

                if (!matcher.find()) {
                    sendJson(exchange, 400, "{\"error\":\"name is required\"}");
                    return;
                }

                String name = matcher.group(1);

                try (Connection c = DriverManager.getConnection(url, user, pass);
                     PreparedStatement p = c.prepareStatement(
                         "INSERT INTO tags(name) VALUES (?) RETURNING id")) {
                    p.setString(1, name);

                    try (ResultSet r = p.executeQuery()) {
                        r.next();
                        int id = r.getInt(1);
                        sendJson(exchange, 201,
                            "{\"id\":" + id + ",\"name\":\"" +
                            name.replace("\"", "\\\"") + "\"}");
                    }
                }
                return;
            }

            if (method.equals("GET")) {
                StringBuilder b = new StringBuilder("[");

                try (Connection c = DriverManager.getConnection(url, user, pass);
                     Statement s = c.createStatement();
                     ResultSet r = s.executeQuery(
                         "SELECT id,name FROM tags ORDER BY id")) {

                    while (r.next()) {
                        if (b.length() > 1) b.append(",");
                        b.append("{\"id\":").append(r.getInt("id"))
                         .append(",\"name\":\"")
                         .append(r.getString("name").replace("\"", "\\\""))
                         .append("\"}");
                    }
                }

                sendJson(exchange, 200, b.append("]").toString());
                return;
            }

            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"internal server error\"}");
        }
    }

    static void sendJson(HttpExchange exchange, int code, String body)
            throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
