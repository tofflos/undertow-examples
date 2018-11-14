package com.example;

import io.undertow.util.Headers;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
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
    public void getWelcomeFile() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().map()).contains(Map.entry(Headers.LOCATION_STRING, List.of("/ui"))).doesNotContainKey(Headers.SET_COOKIE_STRING);
        assertThat(response.body()).isNullOrEmpty();
    }

    @Test
    public void getAPI() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/api")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().map()).doesNotContainKey(Headers.SET_COOKIE_STRING);
        assertThat(response.body()).isEqualTo("API");
    }

    @Test
    public void getUI() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/ui")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().map()).containsKey(Headers.SET_COOKIE_STRING);
        assertThat(response.body()).isEqualTo("UI");
    }
}
