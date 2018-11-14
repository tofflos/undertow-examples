package com.example;

import io.undertow.Undertow;

public class Application {

    private Undertow undertow;
    
    public static void main(String[] args) {
        new Application().start();
    }
    
    public void start() {
        undertow = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(exchange -> {
                    exchange.getResponseSender().send("Hello world!");
                })
                .build();
        
        undertow.start();
    }
    
    public void stop() {
        if(undertow != null) {
            undertow.stop();
        }
    }
}
