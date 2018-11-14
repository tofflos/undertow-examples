## Getting started with Undertow
Recently I tried writing an application directly on top of Undertow. Compared to Jakarta EE you get fast startup times and a small distributable. The downside is that you have figure out how to provide functionality that comes out of the box in a mature framework. There's also less material to be had on what constitutes good practices. But one thing that struck me was how approachable this style of development was.  I got to spend more time gluing libraries together and less time banging my head against the wall trying to understand what my framework is trying to do for me.

Get started by adding the following Maven dependency:
```XML
<dependency>
    <groupId>io.undertow</groupId>
    <artifactId>undertow-core</artifactId>
    <version>2.0.14.Final</version>
</dependency>
```

Write the following code:
```Java
public static void main(String[] args) {
    var undertow = Undertow.builder()
            .addHttpListener(8080, "localhost")
            .setHandler(exchange -> {
                exchange.getResponseSender().send("Hello world!");
            })
            .build();
    
    undertow.start();
}
```

Test it in your terminal using HTTPie:
```Powershell
$ http :8080

HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 12
Date: Wed, 14 Nov 2018 21:29:11 GMT

Hello world!
```

Write an integration test based on AssertJ and the new HTTP client that comes bundled with JDK11:
```Java
@Test
public void get() throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:8080")).build();
    var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        
    assertThat(response.body()).isEqualTo("Hello world!");
}
```

The distributables weigh in at 2.82 MB and the application starts in less than a second.
