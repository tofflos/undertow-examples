package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    public void get() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/static/index.html")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());

        assertThat(response.body()).isEqualTo(Files.readString(Paths.get("./src/main/resources/webroot/index.html")));
    }

    @Test
    public void getWelcomeFile() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080")).build();
        var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, BodyHandlers.ofString());

        assertThat(response.body()).isEqualTo(Files.readString(Paths.get("./src/main/resources/webroot/index.html")));
    }
}
