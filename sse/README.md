## Getting started with Server Sent Events on Undertow

Get started by adding the following Maven dependency:
```XML
<dependency>
    <groupId>io.undertow</groupId>
    <artifactId>undertow-core</artifactId>
    <version>2.0.19.Final</version>
</dependency>
```

Write the following code:
```Java
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
```

Test it in your terminal using HTTPie:
```Bash
$ http --stream :8080
HTTP/1.1 200 OK
Connection: close
Content-Type: text/event-stream; charset=UTF-8
Date: Mon, 18 Mar 2019 21:55:34 GMT

data:0

data:1

data:2

data:3

data:4

data:5

data:6

data:7

data:8

data:9
```

Write an integration test based on AssertJ and a dedicated SSE client library such as the JAX-RS 2.1 SSE client. The built-in HTTP client that comes bundled with JDK11 provides BodyHandlers for types `Stream<String>` and `Publisher<List<ByteBuffer>>` but these require too much effort implementing parsing logic for my taste. I'm also using the Awaitility library to simplify the process of testing asynchronous methods but would have preferred using some sort of deterministic programmatically controllable clock. Unfortunately I couldn't find a library for this that I really liked. 
```Java
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
```
