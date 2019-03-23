package com.example;

import static io.undertow.Handlers.serverSentEvents;
import io.undertow.Undertow;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Application {

    private ScheduledExecutorService scheduler;
    private Undertow undertow;

    public static void main(String[] args) throws InterruptedException {
        new Application().start();
    }

    public void start() throws InterruptedException {
        var serverSentEventHandler = serverSentEvents();

        undertow = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(serverSentEventHandler)
                .build();

        undertow.start();

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(new Runnable() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public void run() {
                serverSentEventHandler.getConnections().forEach(connection -> connection.send(Integer.toString(counter.getAndIncrement())));
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (undertow != null) {
            undertow.stop();
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}