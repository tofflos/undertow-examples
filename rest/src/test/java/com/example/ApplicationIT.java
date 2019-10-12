package com.example;

import com.example.business.items.CreateItemCommand;
import com.example.business.items.Item;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.json.bind.JsonbBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApplicationIT {

    private static Application application;

    @BeforeAll
    public static void setUpClass() {
        application = new Application();
        application.start();
    }

    @AfterAll
    public static void tearDownClass() {
        if (application != null) {
            application.stop();
        }
    }

    @Test
    public void getStaticResource() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/static/index.html")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().map()).contains(Map.entry("Content-Type", List.of("text/html")));
        assertThat(response.body()).isEqualTo(Files.readString(Paths.get("./src/main/resources/webroot/static/index.html")));
    }

    @Test
    public void get() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/items")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        List<Item> body = JsonbBuilder.create().fromJson(response.body(), new ArrayList<Item>() {
        }.getClass().getGenericSuperclass());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().map()).contains(Map.entry("Content-Type", List.of("application/json")));
        assertThat(body).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(new Item(1L, "Item 1"), new Item(2L, "Item 2"), new Item(3L, "Item 3"));
    }

    @Test
    public void getById() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/items/1")).build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        var body = JsonbBuilder.create().fromJson(response.body(), Item.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().map()).contains(Map.entry("Content-Type", List.of("application/json")));
        assertThat(body).isEqualToComparingFieldByField(new Item(1L, "Item 1"));
    }

    @Test
    public void post() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/items"))
                .setHeader("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(JsonbBuilder.create().toJson(new CreateItemCommand("Item 4"))))
                .build();
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        var body = response.body();

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().map()).contains(Map.entry("Location", List.of("/api/items/4")));
        assertThat(body).isNullOrEmpty();
    }
}
