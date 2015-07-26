package com.az.ip.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Created by magnus on 21/07/15.
 */
public final class SSLUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SSLUtil.class);
    private static final String warnMsg = "### ONLY TO BE USED IN DEVELOPMENT AND TEST ###";

    private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers(){
                LOG.warn("UNQUESTIONING_TRUST_MANAGER return null Accepted Issuers. " + warnMsg);
                return null;
            }
            public void checkClientTrusted( X509Certificate[] certs, String authType ) {
                LOG.warn("UNQUESTIONING_TRUST_MANAGER approves client certificate without checking. " + warnMsg);
            }
            public void checkServerTrusted( X509Certificate[] certs, String authType ) {
                LOG.warn("UNQUESTIONING_TRUST_MANAGER approves server certificate without checking. " + warnMsg);
            }
        }
    };

    public static void registerKeyStore(String keyStoreName) {
        try {
            LOG.info("Load server certificates from classpath: '" + keyStoreName + "'");

            ClassLoader classLoader = PatientIntegrationTests.class.getClassLoader();
            InputStream keyStoreInputStream = classLoader.getResourceAsStream(keyStoreName);
            if (keyStoreInputStream == null) {
                throw new FileNotFoundException("Could not find file named '" + keyStoreName + "' in the CLASSPATH");
            }

            //load the keystore
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(keyStoreInputStream, null);

            //add to known keystore
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            //default SSL connections are initialized with the keystore above
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManagers, null);
            SSLContext.setDefault(sc);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static void turnOffSslChecking() {
        try {
            // Install the all-trusting trust manager
            LOG.warn("Turning Off SSL Checking. " + warnMsg);
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init( null, UNQUESTIONING_TRUST_MANAGER, null );
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static void turnOnSslChecking() {
        try {
            // Return it to the initial state (discovered by reflection, now hardcoded)
            LOG.info("Turning On SSL Checking.");
            SSLContext.getInstance("SSL").init( null, null, null );
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private SSLUtil(){
        throw new UnsupportedOperationException( "Do not instantiate libraries.");
    }
}