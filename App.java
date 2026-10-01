import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class App {

    // Хранилище заметок в памяти
    static class Note {
        int id;
        String text;
        Note(int id, String text) { this.id = id; this.text = text; }
    }

    static List<Note> notes = new ArrayList<>();
    static AtomicInteger counter = new AtomicInteger(1);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // GET /notes — получить все заметки
        server.createContext("/notes", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Если путь ровно /notes (список или создание)
            if (path.equals("/notes")) {
                if (method.equals("GET")) {
                    sendJson(exchange, 200, notesToJson());
                } else if (method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String text = extractText(body);
                    if (text == null) {
                        sendJson(exchange, 400, "{\"error\":\"text is required\"}");
                        return;
                    }
                    Note note = new Note(counter.getAndIncrement(), text);
                    notes.add(note);
                    sendJson(exchange, 201, noteToJson(note));
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
                return;
            }

            // Если путь /notes/{id} (обновление или удаление)
            if (path.startsWith("/notes/")) {
                String idStr = path.substring("/notes/".length());
                int id;
                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"invalid id\"}");
                    return;
                }

                Note found = null;
                for (Note n : notes) {
                    if (n.id == id) { found = n; break; }
                }

                if (found == null) {
                    sendJson(exchange, 404, "{\"error\":\"note not found\"}");
                    return;
                }

                if (method.equals("PUT")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String text = extractText(body);
                    if (text == null) {
                        sendJson(exchange, 400, "{\"error\":\"text is required\"}");
                        return;
                    }
                    found.text = text;
                    sendJson(exchange, 200, noteToJson(found));
                } else if (method.equals("DELETE")) {
                    notes.remove(found);
                    sendJson(exchange, 200, "{\"status\":\"deleted\"}");
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"not found\"}");
        });

        server.setExecutor(null);
        System.out.println("Сервер запущен на порту 8080...");
        server.start();
    }

    // Отправка JSON-ответа
    static void sendJson(com.sun.net.httpserver.HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // Превращаем одну заметку в JSON
    static String noteToJson(Note n) {
        return "{\"id\":" + n.id + ",\"text\":\"" + n.text + "\"}";
    }

    // Превращаем список заметок в JSON
    static String notesToJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < notes.size(); i++) {
            sb.append(noteToJson(notes.get(i)));
            if (i < notes.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // Простейшее вытаскивание поля "text" из JSON-строки {"text":"..."}
    static String extractText(String body) {
        int idx = body.indexOf("\"text\"");
        if (idx == -1) return null;
        int start = body.indexOf("\"", idx + 6);
        if (start == -1) return null;
        int end = body.indexOf("\"", start + 1);
        if (end == -1) return null;
        return body.substring(start + 1, end);
    }
}
