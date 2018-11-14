package com.example;

import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApplicationIT {

    private static Application application;

    @BeforeClass
    public static void setUpClass() {
        application = new Application();
        application.start();
    }

    @AfterClass
    public static void tearDownClass() {
        if (application != null) {
            application.stop();
        }
    }

    @Test
    public void getStatic() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/static/index.html")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(StatusCodes.OK);
        assertThat(response.headers().map()).contains(Map.entry(Headers.CONTENT_TYPE_STRING, List.of("text/html")));
        assertThat(response.body()).isEqualTo(Files.readString(Paths.get("./src/main/resources/webroot/static/index.html")));
    }

    @Test
    public void getTemplate() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/templates/index.hbs")).build();
        var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(StatusCodes.OK);
        assertThat(response.headers().map()).contains(Map.entry(Headers.CONTENT_TYPE_STRING, List.of("text/html")));
        assertThat(response.body()).isEqualTo(Files.readString(Paths.get("./src/main/resources/webroot/static/index.html")));
    }
}
