package com.example;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;

public class Application {

    private Undertow undertow;
    
    public static void main(String[] args) {
        new Application().start();
    }
    
    public void start() {
        var handler = Handlers.path()
                .addExactPath("/", Handlers.redirect("/ui"))
                .addPrefixPath("/api",
                        exchange -> exchange.getResponseSender().send("API"))
                .addPrefixPath("/ui", 
                        new SessionAttachmentHandler(new SessionCreationHandler(exchange -> exchange.getResponseSender().send("UI")), 
                                new InMemorySessionManager("SESSION_MANAGER"), new SessionCookieConfig()));
        
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

class SessionCreationHandler implements HttpHandler {

    private final HttpHandler next;

    public SessionCreationHandler(HttpHandler next) {
        this.next = next;
    }
    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        var manager = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
        var config = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);
        var session = manager.getSession(exchange, config);
        
        if(session == null) {
            manager.createSession(exchange, config);
        }
        
        next.handleRequest(exchange);
    }
}