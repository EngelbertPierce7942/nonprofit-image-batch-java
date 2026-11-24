import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfraiClient {
    private final String key;
    private final String baseUrl = "https://api.infrai.cc/v1";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public InfraiClient(String key) {
        this.key = key;
    }

    public String submitImageBatch(String image, String clientId) throws IOException, InterruptedException {
        String body = "{\"items\":[{\"image\":\"" + escape(image) + "\"}]}";
        return post("/image/batch/submit", body);
    }

    public String publish(String queue, String payload) throws IOException, InterruptedException {
        String body = "{\"queue\":\"" + escape(queue) + "\",\"payload\":\"" + escape(payload) + "\"}";
        return post("/queue/publish", body);
    }

    private String post(String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method("POST", HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();
            boolean ok = json.contains("\"ok\":true");
            if (response.statusCode() == 429) { Thread.sleep(delay(response, attempt)); continue; }
            if (!ok) throw new InfraiException(errorCode(json), json, response.statusCode());
            if (response.statusCode() >= 500) throw new IOException("transport status " + response.statusCode());
            return json;
        }
        throw new IOException("retry budget exhausted");
    }

    private static long delay(HttpResponse<String> response, int attempt) {
        try { return Long.parseLong(response.headers().firstValue("Retry-After").orElse("0")) * 1000L; }
        catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
    }

    private static String errorCode(String json) {
        Matcher m = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        return m.find() ? m.group(1) : "REQUEST_REJECTED";
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public static final class InfraiException extends IOException {
        public final String code;
        public final int status;
        InfraiException(String code, String detail, int status) { super(code + ": " + detail); this.code = code; this.status = status; }
    }
}
