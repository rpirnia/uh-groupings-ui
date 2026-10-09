package edu.hawaii.its.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class HttpRequestServiceTest {

    private HttpRequestService httpRequestService;
    private HttpServer server;
    private String apiBase;

    @BeforeEach
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        apiBase = "http://localhost:" + server.getAddress().getPort();
        httpRequestService = new HttpRequestService(apiBase);
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void makeApiRequestThrowsWithApiErrorStatusAndBody() {
        String body = "{"
                + "\"status\":503,"
                + "\"resultCode\":\"BACKEND_UNAVAILABLE\","
                + "\"message\":\"Groupings data is temporarily unavailable. Please try again later.\","
                + "\"path\":\"/api/groupings/groupings\","
                + "\"timestamp\":\"2026-06-16T12:00:00\""
                + "}";
        server.createContext("/api/groupings/groupings", exchange -> sendJson(exchange, HttpStatus.SERVICE_UNAVAILABLE, body));

        WebClientResponseException exception = assertThrows(WebClientResponseException.class,
                () -> httpRequestService.makeApiRequest(
                        apiBase + "/api/groupings/groupings",
                        HttpMethod.GET));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals(body, exception.getResponseBodyAsString());
    }

    @Test
    public void makeApiRequestAcceptsRelativePath() {
        String body = "{\"resultCode\":\"SUCCESS\"}";
        server.createContext("/api/groupings/groupings", exchange -> sendJson(exchange, HttpStatus.OK, body));

        ResponseEntity<String> response = httpRequestService.makeApiRequest(
                "/api/groupings/groupings",
                HttpMethod.GET);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body, response.getBody());
    }

    @Test
    public void toRelativePathStripsConfiguredBase() {
        assertEquals("/members", httpRequestService.toRelativePath(apiBase + "/members"));
        assertEquals("/", httpRequestService.toRelativePath(apiBase + "/"));
        assertEquals("/", httpRequestService.toRelativePath(apiBase));
    }

    @Test
    public void resolveApiUriUsesConfiguredBaseOnly() {
        assertEquals(apiBase + "/members",
                httpRequestService.resolveApiUri(apiBase + "/members").toString());
        assertEquals(apiBase + "/members",
                httpRequestService.resolveApiUri("/members").toString());
        assertEquals(apiBase + "/groupings/group?page=1&size=2",
                httpRequestService.resolveApiUri(apiBase + "/groupings/group?page=1&size=2").toString());
    }

    @Test
    public void resolveApiUriPreservesConfiguredBasePath() {
        HttpRequestService service = new HttpRequestService("http://localhost:8081/uhgroupingsapi/api/groupings/v2.1");

        assertEquals("http://localhost:8081/uhgroupingsapi/api/groupings/v2.1/members",
                service.resolveApiUri("http://localhost:8081/uhgroupingsapi/api/groupings/v2.1/members").toString());
        assertEquals("http://localhost:8081/uhgroupingsapi/api/groupings/v2.1/",
                service.resolveApiUri("http://localhost:8081/uhgroupingsapi/api/groupings/v2.1/").toString());
    }

    @Test
    public void rejectsTraversalInAbsoluteAndRelativePaths() {
        String base = apiBase + "/api/groupings/v2.1";
        HttpRequestService service = new HttpRequestService(base);
        String[] paths = { "/../admin", "/members/../../admin", "/./members",
                "/%2e%2e/admin", "/.%2E/admin", "/members%2f..%2f..%2fadmin",
                "/%2e%2e%5cadmin" };

        for (String path : paths) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.toRelativePath(path), path);
            assertThrows(IllegalArgumentException.class,
                    () -> service.toRelativePath(base + path), path);
            assertThrows(IllegalArgumentException.class,
                    () -> service.resolveApiUri(path), path);
            assertThrows(IllegalArgumentException.class,
                    () -> service.resolveApiUri(base + path), path);
        }
    }

    @Test
    public void preservesSafePathsAndQueryValues() {
        String base = apiBase + "/api/groupings/v2.1";
        HttpRequestService service = new HttpRequestService(base + "/");

        assertEquals(base + "/", service.resolveApiUri(base).toString());
        assertEquals(base + "/members", service.resolveApiUri("/members").toString());
        assertEquals(base + "/members..json?next=/../admin",
                service.resolveApiUri("/members..json?next=/../admin").toString());
    }

    @Test
    public void constructorRequiresApiBase() {
        assertThrows(IllegalArgumentException.class,
                () -> new HttpRequestService(null));
        assertThrows(IllegalArgumentException.class,
                () -> new HttpRequestService(" "));
    }

    @Test
    public void toRelativePathRejectsForeignHost() {
        assertThrows(IllegalArgumentException.class,
                () -> httpRequestService.toRelativePath("http://evil.example/members"));
        assertThrows(IllegalArgumentException.class,
                () -> httpRequestService.toRelativePath("//evil.example/members"));
        assertThrows(IllegalArgumentException.class,
                () -> httpRequestService.resolveApiUri("http://evil.example/members"));
    }

    @Test
    public void toRelativePathRejectsPrefixThatIsNotBaseBoundary() {
        HttpRequestService service = new HttpRequestService("http://localhost:8081/api");

        assertThrows(IllegalArgumentException.class,
                () -> service.toRelativePath("http://localhost:8081/apix/members"));
        assertEquals("/members", service.toRelativePath("http://localhost:8081/api/members"));
    }

    private void sendJson(HttpExchange exchange, HttpStatus status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status.value(), bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
