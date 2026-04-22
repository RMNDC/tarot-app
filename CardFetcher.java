import java.io.*;
import java.net.*;
import java.util.*;

// Calls the external Tarot API and parses the card data
public class CardFetcher {

    static final String API_URL = "https://tarotapi.dev/api/v1/cards";

    public static List<Map<String, String>> fetchCards() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();

            return parseCards(sb.toString());
        } catch (Exception e) {
            System.err.println("API fetch failed: " + e.getMessage());
            return null;
        }
    }

    static List<Map<String, String>> parseCards(String json) {
        List<Map<String, String>> cards = new ArrayList<>();
        for (String part : json.split("\\{\"name\":")) {
            if (part.startsWith("[")) continue;
            String block = "{\"name\":" + part;
            Map<String, String> card = new HashMap<>();
            card.put("name",        field(block, "name"));
            card.put("meaning_up",  field(block, "meaning_up"));
            card.put("meaning_rev", field(block, "meaning_rev"));
            cards.add(card);
        }
        return cards;
    }

    static String field(String json, String key) {
        String tag = "\"" + key + "\":\"";
        int s = json.indexOf(tag);
        if (s == -1) return "";
        s += tag.length();
        int e = json.indexOf("\"", s);
        return e == -1 ? "" : json.substring(s, e).replace("\\u2019", "'").replace("\\n", " ");
    }
}
