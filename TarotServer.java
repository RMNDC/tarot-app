import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class TarotServer {

    static final String[] POSITIONS = { "Past", "Present", "Future" };
    static final String CPP_URL = "http://localhost:9090/pick?count=";

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", TarotServer::serveHome);
        server.createContext("/draw", TarotServer::serveDraw);
        server.setExecutor(null);
        server.start();
        System.out.println("Java server running on port " + port);
    }

    // Serves index.html to the browser
    static void serveHome(HttpExchange ex) throws IOException {
        byte[] body = TarotServer.class.getResourceAsStream("/index.html").readAllBytes();
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    // Fetches cards from tarotapi.dev, asks C++ for 3 picks, returns JSON
    static void serveDraw(HttpExchange ex) throws IOException {
        // Step 1: Java calls external Tarot API
        List<Map<String, String>> cards = CardFetcher.fetchCards();
        if (cards == null) {
            send(ex, 500, "{\"error\":\"Failed to fetch cards.\"}");
            return;
        }

        // Step 2: Java calls C++ server via HTTP to get 3 random indices
        int[] picks = askCppPicker(cards.size());
        if (picks == null) {
            send(ex, 500, "{\"error\":\"C++ picker unavailable.\"}");
            return;
        }

        // Step 3: Build the JSON spread
        StringBuilder json = new StringBuilder("{\"spread\":[");
        for (int i = 0; i < 3; i++) {
            Map<String, String> card = cards.get(picks[i]);
            boolean rev = new Random().nextBoolean();
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

    // Java calls C++ over HTTP — pure API-based communication
    static int[] askCppPicker(int count) {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                new URL(CPP_URL + count).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            String body = reader.readLine();
            reader.close();
            conn.disconnect();

            // Parse {"picks":[a,b,c]}
            String inner = body.replaceAll(".*\\[|\\].*", "");
            String[] parts = inner.split(",");
            return new int[]{
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim())
            };
        } catch (Exception e) {
            System.err.println("C++ picker error: " + e.getMessage());
            return null;
        }
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
