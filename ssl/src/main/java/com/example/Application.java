package com.example;

import io.undertow.Undertow;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class Application {

    private Undertow undertow;
    
    public static void main(String[] args) throws Exception {
        new Application().start();
    }
    
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
    
    public void stop() {
        if(undertow != null) {
            undertow.stop();
        }
    }
}
