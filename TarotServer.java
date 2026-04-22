import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class TarotServer {

    static final String[] POSITIONS = { "Past", "Present", "Future" };
    static final Random RNG = new Random();

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", TarotServer::serveHome);
        server.createContext("/draw", TarotServer::serveDraw);
        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port " + port);
    }

    // Serves index.html to the browser
    static void serveHome(HttpExchange ex) throws IOException {
        byte[] body = TarotServer.class.getResourceAsStream("/index.html").readAllBytes();
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    // Fetches cards from API, picks 3 via C++, returns JSON
    static void serveDraw(HttpExchange ex) throws IOException {
        List<Map<String, String>> cards = CardFetcher.fetchCards();
        if (cards == null) {
            send(ex, 500, "{\"error\":\"Failed to fetch cards.\"}");
            return;
        }

        // C++ picks 3 random indices via JNI
        int[] picks = new CardPickerNative().pickThree(cards.size());

        StringBuilder json = new StringBuilder("{\"spread\":[");
        for (int i = 0; i < 3; i++) {
            Map<String, String> card = cards.get(picks[i]);
            boolean rev = RNG.nextBoolean();
            String meaning = rev ? card.get("meaning_rev") : card.get("meaning_up");
            if (meaning == null || meaning.isEmpty()) meaning = "No meaning found.";
            if (i > 0) json.append(",");
            json.append("{\"position\":\"").append(POSITIONS[i]).append("\",")
                .append("\"name\":\"").append(esc(card.get("name"))).append("\",")
                .append("\"orientation\":\"").append(rev ? "Reversed" : "Upright").append("\",")
                .append("\"meaning\":\"").append(esc(meaning)).append("\"}");
        }
        json.append("]}");
        send(ex, 200, json.toString());
    }

    static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
