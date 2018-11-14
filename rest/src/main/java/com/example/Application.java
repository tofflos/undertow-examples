package com.example;

import static com.example.JsonbSerializationHandler.ATTACHMENT_KEY;
import com.example.business.items.CreateItemCommand;
import com.example.business.items.ItemRepository;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.util.NoSuchElementException;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;

public class Application {

    public static final ResponseCodeHandler HANDLE_422 = new ResponseCodeHandler(422);

    private Jsonb jsonb;
    private ItemRepository repository;
    private Undertow undertow;

    public static void main(String[] args) {
        new Application().start();
    }

    public void start() {

        var apiRoutes = Handlers.routing()
                .get("/items", this::get)
                .get("/items/{id}", this::getById)
                .post("/items", this::post);

        var rootHandler = Handlers.path()
                .addExactPath("/", Handlers.redirect("/static/index.html"))
                .addPrefixPath("/static", new ResourceHandler(new ClassPathResourceManager(Application.class.getClassLoader(), "webroot/static")))
                .addPrefixPath("/api", new JsonbSerializationHandler(apiRoutes));

        jsonb = JsonbBuilder.create();

        repository = new ItemRepository();

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

    private void get(HttpServerExchange exchange) {
        exchange.putAttachment(JsonbSerializationHandler.ATTACHMENT_KEY, repository.getItems());
    }

    private void getById(HttpServerExchange exchange) throws Exception {
        try {
            var id = Long.parseLong(exchange.getQueryParameters().get("id").pop());
            var item = repository.getById(id).orElseThrow();

            exchange.putAttachment(JsonbSerializationHandler.ATTACHMENT_KEY, item);
        } catch (NumberFormatException | NoSuchElementException ex) {
            ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
        }
    }

    private void post(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this::post);
            return;
        }

        try {
            if (exchange.getRequestContentLength() > 0) {
                exchange.startBlocking();
                var command = jsonb.fromJson(exchange.getInputStream(), CreateItemCommand.class);
                var item = repository.create(command);
                exchange.setStatusCode(StatusCodes.CREATED);
                exchange.getResponseHeaders().put(Headers.LOCATION, exchange.getRequestPath() + "/" + item.getId());
                exchange.endExchange();
            }
        } catch (JsonbException ex) {
            HANDLE_422.handleRequest(exchange);
        }
    }
}

class JsonbSerializationHandler implements HttpHandler {

    public static final AttachmentKey ATTACHMENT_KEY = AttachmentKey.create(Object.class);

    private final Jsonb jsonb;
    private final HttpHandler next;

    public JsonbSerializationHandler(HttpHandler next) {
        this.jsonb = JsonbBuilder.create();
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        next.handleRequest(exchange);

        Object object = exchange.getAttachment(ATTACHMENT_KEY);

        if (object != null) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.startBlocking();
            jsonb.toJson(object, exchange.getOutputStream());
            exchange.endExchange();
        }
    }
}
