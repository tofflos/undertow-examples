## Setting up SSL on Undertow

1. Generate a keypair using keytool and store it in a keystore. The default keystore format was changed from JKS to PKCS12 in Java 9. See JEP 229 https://openjdk.java.net/jeps/229. The keystore will contain a private key and a public key. The public key is what we will use as the server certificate.
2. Export the server certificate into a PEM format file for use with HTTPie.
3. Import the server certificate into a truststore for use with HTTP clients written in Java. The truststore is just another keystore in PKCS12 format that contains the server certificate but not the server private key.

```Powershell
keytool -genkeypair -keystore keystore.pkcs12 -keyalg RSA -dname "CN=localhost"
keytool -exportcert -keystore keystore.pkcs12 -rfc -file certificate.pem
keytool -importcert -keystore truststore.pkcs12 -file certificate.pem
```

If you prefer using OpenSSL you can try the following steps below. I was able to get the keystore and the certificate.pem working but not the truststore so my HTTP client written in Java fails with java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty. From what I can tell you need to add "Bag Attribute" 2.16.840.1.113894.746875.1.1 to the truststore and there doesn't seem to be a simple way of doing that with OpenSSL. See https://stackoverflow.com/questions/42766935/creating-p12-truststore-with-openssl. I tried this on early access build 19 for Java 12.
```Bash
openssl req -x509 -newkey rsa:2048 -subj '/CN=localhost' -keyout key.pem -out certificate.pem
openssl pkcs12 -export -out keystore.pkcs12 -inkey key.pem -in certificate.pem
openssl pkcs12 -export -out truststore.pkcs12 -nokeys -in certificate.pem
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
http --verify=certificate.pem https://localhost:8443
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