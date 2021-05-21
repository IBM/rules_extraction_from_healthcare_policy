package com.ibm.wh.extractionservice.support;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;

public class SslContextUtils {

    private SslContextUtils() {
        // utility class
    }

    public static SSLContext sslContextWithTrustAllStrategy() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        // FIXME: use SSLContext API and remove dependency from `org.apache.httpcomponents:httpclient`
        return SSLContexts.custom()
                .loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true)
                .build();
    }

}
