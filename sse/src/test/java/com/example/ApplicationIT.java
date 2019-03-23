package com.example;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.sse.SseEventSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApplicationIT {

    private static Application application;

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
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
    public void get() {
        var client = ClientBuilder.newBuilder().build();
        var target = client.target("http://localhost:8080");
        var source = SseEventSource.target(target).build();
        var events = new ArrayList<Integer>();

        source.register(event -> events.add(Integer.parseInt(event.readData())));
        source.open();
        await().atMost(15, TimeUnit.SECONDS).until(() -> events.size() == 10);
        source.close();

        assertThat(events).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }
}
