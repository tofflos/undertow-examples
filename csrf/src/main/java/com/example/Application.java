package com.example;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

public class Application {

    private Undertow undertow;
    private FormParserFactory formParserFactory;

    public static void main(String[] args) {
        new Application().start();
    }

    public void start() {
        var templateRoutes = Handlers.path()
                .addExactPath("/index.hbs", this::index);

        this.simple(exchange)
        
        Function function = () -> Handlers.redirect("/templates/index.hbs");
        String s = "";

        switch (s) {
            case :
        }

        var rootHandler = Handlers.path()
                .addExactPath("/", Handlers.redirect("/templates/index.hbs"))
                .addPrefixPath("/templates",
                        new SessionAttachmentHandler(new SessionCreationHandler(new EagerFormParsingHandler(new CsrfHandler(new HandlebarsHandler(templateRoutes, "/webroot", "")))),
                                new InMemorySessionManager("SESSION_MANAGER"), new SessionCookieConfig()));

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
        var manager = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
        var config = exchange.getAttachment(SessionCookieConfig.ATTACHMENT_KEY);
        var session = manager.getSession(exchange, config);
        var csrf = session.getAttribute("CSRF_TOKEN");

        exchange.putAttachment(HandlebarsHandler.ATTACHMENT_KEY, Map.of("message", "Hello world!", "csrf", csrf));
    }

    private HttpHandler simple(HttpServerExchange exchange) {

        String s = exchange.getRelativePath();
        
        if ("s".equals("/")) {
            return Handlers.redirect("/templates/index.hbs");
        } else if (s.startsWith("/static")) {
            return Handlers.resource(ResourceManager.EMPTY_RESOURCE_MANAGER);
        } else if( s.endsWith(".hbs")) {
            return exchange2 -> {};
        } else {
            return ResponseCodeHandler.HANDLE_404;
        }
    }
}

class SimplePathHandler implements Http



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

class CsrfHandler implements HttpHandler {

    public static final AttachmentKey ATTACHMENT_KEY = AttachmentKey.create(String.class);

    private final HttpHandler next;

    public CsrfHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        System.out.println("Entering CSRF handler.");

        if (Methods.POST.equals(exchange.getRequestMethod())) {
            var manager = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
            var config = exchange.getAttachment(SessionCookieConfig.ATTACHMENT_KEY);
            var session = manager.getSession(exchange, config);
            var expectedToken = session.getAttribute("CSRF_TOKEN");
            var formData = exchange.getAttachment(FormDataParser.FORM_DATA);
            var actualToken = formData.get("csrf").stream().findAny().orElse(null).getValue();

            System.out.println("Expected CSRF token: " + expectedToken);
            System.out.println("Actual CSRF token: " + actualToken);

            if (!expectedToken.equals(actualToken)) {
                ResponseCodeHandler.HANDLE_403.handleRequest(exchange);
                return;
            }
        }

        next.handleRequest(exchange);
    }
}

class SessionCreationHandler implements HttpHandler {

    private final HttpHandler next;
    private final SecureRandom secureRandom;

    public SessionCreationHandler(HttpHandler next) {
        this.next = next;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        var manager = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
        var config = exchange.getAttachment(SessionCookieConfig.ATTACHMENT_KEY);
        var session = manager.getSession(exchange, config);

        if (session == null) {
            session = manager.createSession(exchange, config);

            var values = new byte[20];
            secureRandom.nextBytes(values);
            var token = Base64.getEncoder().encodeToString(values);

            System.out.println("Setting CSRF token: " + token);

            session.setAttribute("CSRF_TOKEN", token);
        }

        next.handleRequest(exchange);
    }
}
