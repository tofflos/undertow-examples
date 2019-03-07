package com.example;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApplicationIT {

    private static Application application;

    @BeforeClass
    public static void setUpClass() throws Exception {
        application = new Application();
        application.start();
    }

    @AfterClass
    public static void tearDownClass() {
        if (application != null) {
            application.stop();
        }
    }

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
}
