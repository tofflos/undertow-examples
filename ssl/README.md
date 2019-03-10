Create a key pair using keytool:
```Powershell
keytool -genkeypair -keystore keystore.pkcs12 -keyalg RSA -dname "CN=localhost"
keytool -exportcert -keystore keystore.pkcs12 -rfc -file server-side-cert.pem
keytool -importcert -keystore truststore.pkcs12 -file server-side-cert.pem
```

Create an SSLContext using the keystore and launch Undertow:
```Java
public void start() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
    var keyStore = KeyStore.getInstance(new File("./keystore.pkcs12"), "secret".toCharArray());
    var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, "secret".toCharArray());
    var keyManagers = keyManagerFactory.getKeyManagers();

    var context = SSLContext.getInstance("TLS");
    context.init(keyManagers, null, SecureRandom.getInstanceStrong());
    
    undertow = Undertow.builder()
            .addHttpsListener(8443, "localhost", context)
            .setHandler(exchange -> {
                exchange.getResponseSender().send("Hello world!");
            })
            .build();
    
    undertow.start();
}
```

List supported SSL ciphers:
```Bash
nmap --script ssl-enum-ciphers -p 8443 localhost
Starting Nmap 7.60 ( https://nmap.org ) at 2019-03-07 00:04 STD
Nmap scan report for localhost (127.0.0.1)
Host is up (0.88s latency).

PORT     STATE SERVICE
8443/tcp open  https-alt
| ssl-enum-ciphers:
|   TLSv1.0:
|     ciphers:
|       TLS_DHE_RSA_WITH_AES_128_CBC_SHA (dh 2048) - A
|       TLS_DHE_RSA_WITH_AES_256_CBC_SHA (dh 2048) - A
|       TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA (secp256k1) - A
|       TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA (secp256k1) - A
|       TLS_RSA_WITH_AES_128_CBC_SHA (rsa 2048) - A
|       TLS_RSA_WITH_AES_256_CBC_SHA (rsa 2048) - A
|     compressors:
|       NULL
|     cipher preference error: Error when comparing TLS_DHE_RSA_WITH_AES_128_CBC_SHA and TLS_DHE_RSA_WITH_AES_256_CBC_SHA
|   TLSv1.1:
|     ciphers:
|       TLS_DHE_RSA_WITH_AES_128_CBC_SHA (dh 2048) - A
|       TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA (secp256k1) - A
|       TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA (secp256k1) - A
|       TLS_RSA_WITH_AES_128_CBC_SHA (rsa 2048) - A
|       TLS_RSA_WITH_AES_256_CBC_SHA (rsa 2048) - A
|     compressors:
|       NULL
|     cipher preference: client
|   TLSv1.2:
|     ciphers:
|       TLS_DHE_RSA_WITH_AES_128_CBC_SHA (dh 2048) - A
|       TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 (dh 2048) - A
|       TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 (dh 2048) - A
|       TLS_DHE_RSA_WITH_AES_256_CBC_SHA (dh 2048) - A
|       TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 (dh 2048) - A
|       TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256 (dh 2048) - A
|       TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA (secp256k1) - A
|       TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 (secp256k1) - A
|       TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 (secp256k1) - A
|       TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 (secp256k1) - A
|       TLS_RSA_WITH_AES_128_CBC_SHA (rsa 2048) - A
|       TLS_RSA_WITH_AES_128_CBC_SHA256 (rsa 2048) - A
|       TLS_RSA_WITH_AES_128_GCM_SHA256 (rsa 2048) - A
|       TLS_RSA_WITH_AES_256_CBC_SHA (rsa 2048) - A
|       TLS_RSA_WITH_AES_256_CBC_SHA256 (rsa 2048) - A
|       TLS_RSA_WITH_AES_256_GCM_SHA384 (rsa 2048) - A
|     compressors:
|       NULL
|     cipher preference: client
|_  least strength: A

Nmap done: 1 IP address (1 host up) scanned in 206.40 seconds
```

Test it in your terminal using HTTPie:
```Bash
http --verify=server-side-cert.pem https://localhost:8443
HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 12
Date: Sun, 10 Mar 2019 20:52:02 GMT

Hello world!
```

Write an integration test based on AssertJ and the new HTTP client that comes bundled with JDK11:
```Java
@Test
public void get() throws Exception {
    var trustStore = KeyStore.getInstance(new File("./truststore.pkcs12"), "secret".toCharArray());
    var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);
    var trustManagers = trustManagerFactory.getTrustManagers();

    var context = SSLContext.getInstance("TLS");
    context.init(null, trustManagers, SecureRandom.getInstanceStrong());

    var request = HttpRequest.newBuilder(URI.create("https://localhost:8443")).build();
    var response = HttpClient.newBuilder().sslContext(context).build()
            .send(request, BodyHandlers.ofString());

    assertThat(response.body()).isEqualTo("Hello world!");
}
```