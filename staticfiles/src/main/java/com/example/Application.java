package com.example;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import java.nio.file.Paths;

public class Application {

    private Undertow undertow;
    
    public static void main(String[] args) {
        new Application().start();
    }
    
    public void start() {
        var handler = Handlers.path()
                .addExactPath("/", Handlers.redirect("/static"))
                .addPrefixPath("/static", new ResourceHandler(new PathResourceManager(Paths.get("./webroot"))));
        
        undertow = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(handler)
                .build();
        
        undertow.start();
    }
    
    public void stop() {
        if(undertow != null) {
            undertow.stop();
        }
    }
}
