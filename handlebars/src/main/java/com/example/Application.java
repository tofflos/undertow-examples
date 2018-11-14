package com.example;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import java.io.OutputStreamWriter;
import java.util.Map;

public class Application {

    private Undertow undertow;

    public static void main(String[] args) {
        new Application().start();
    }

    public void start() {
        var templateRoutes = Handlers.path()
                .addExactPath("/index.hbs", this::index);

        var rootHandler = Handlers.path()
                .addExactPath("/", Handlers.redirect("/static/index.html"))
                .addPrefixPath("/static", new ResourceHandler(new ClassPathResourceManager(Application.class.getClassLoader(), "webroot/static")))
                .addExactPath("/templates", Handlers.redirect("/templates/index.hbs"))
                .addPrefixPath("/templates", new HandlebarsHandler(templateRoutes, "/webroot", ""));

        undertow = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(rootHandler)
                .build();

        undertow.start();
    }

    public void stop() {
        if (undertow != null) {
            undertow.stop();
        }
    }

    private void index(HttpServerExchange exchange) {
        exchange.putAttachment(HandlebarsHandler.ATTACHMENT_KEY, Map.of("message", "Hello world!"));
    }
}

class HandlebarsHandler implements HttpHandler {

    public static final AttachmentKey ATTACHMENT_KEY = AttachmentKey.create(Map.class);

    private final Handlebars handlebars;
    private final HttpHandler next;

    public HandlebarsHandler(HttpHandler next) {
        this(next, "/", ".hbs");
    }

    public HandlebarsHandler(HttpHandler next, String prefix, String suffix) {
        this.handlebars = new Handlebars(new ClassPathTemplateLoader(prefix, suffix));
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        next.handleRequest(exchange);

        var path = exchange.getResolvedPath();
        var template = handlebars.compile(path);
        var map = exchange.getAttachment(ATTACHMENT_KEY);
        var context = Context.newBuilder(map).resolver(MapValueResolver.INSTANCE).build();

        if (exchange.isInIoThread()) {
            exchange.dispatch(blockingHandler -> {
                blockingHandler.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                blockingHandler.startBlocking();
                var writer = new OutputStreamWriter(blockingHandler.getOutputStream());
                template.apply(context, writer);
                writer.flush();
                blockingHandler.endExchange();
            });
        }
    }
}
