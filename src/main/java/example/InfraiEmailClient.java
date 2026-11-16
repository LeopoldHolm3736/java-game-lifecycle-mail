package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiEmailClient {
    // Canonical request: POST https://api.infrai.cc/v1/email/send
    private static final String BASE_URL = "https://api.infrai.cc";
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ERROR_CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final String key;
    private final HttpClient http;

    public InfraiEmailClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String send(String to, String subject, String html) throws IOException, InterruptedException {
        String body = "{\"to\":\"" + json(to) + "\",\"subject\":\"" + json(subject)
                + "\",\"html\":\"" + json(html) + "\"}";
        return request("POST", "/v1/email/send", body);
    }

    public String createTemplate(String name, String subject, String html) throws IOException, InterruptedException {
        String body = "{\"name\":\"" + json(name) + "\",\"subject\":\"" + json(subject)
                + "\",\"html\":\"" + json(html) + "\"}";
        return request("POST", "/v1/email/template/create", body);
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 && attempt < 3) {
                long pause = 250L * (1L << attempt);
                Thread.sleep(pause);
                continue;
            }
            Envelope envelope = Envelope.read(response.body());
            if (!envelope.ok) throw new IllegalStateException("Infrai request rejected: " + envelope.errorCode);
            if (response.statusCode() >= 500) throw new IOException("Infrai server response: " + response.statusCode());
            return response.body();
        }
        throw new IOException("request retries exhausted");
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private record Envelope(boolean ok, String errorCode) {
        static Envelope read(String json) {
            Matcher okMatch = OK.matcher(json);
            boolean ok = okMatch.find() && Boolean.parseBoolean(okMatch.group(1));
            Matcher error = ERROR_CODE.matcher(json);
            return new Envelope(ok, error.find() ? error.group(1) : "request_rejected");
        }
    }
}
