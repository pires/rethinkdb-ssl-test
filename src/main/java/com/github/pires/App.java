package com.github.pires;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;

public class App {

    /**
     * Default SSL/TLS protocol version
     * <p>
     * This property is defined as String {@value #DEFAULT_SSL_PROTOCOL}
     */
    private static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";

    /**
     * @param args
     */
    public static void main(String... args) {
        if (args.length < 2) {
            System.err.println("To test unecure: mvn exec:java <hostname> <port> <authKey> <keystore>");
            System.err.println("To test secure: mvn exec:java <hostname> <port> <authKey> <keystore>");
            System.exit(1);
        }

        // prepare RethinkDB connection builder
        final RethinkDB r = RethinkDB.r;
        Connection conn = null;
        Connection.Builder builder = r.connection().hostname(args[0]).port(Integer.parseInt(args[1]));

        // is auth-key set?
        if (args.length > 2) {
            builder.authKey(args[2]);
        }

        // is SSL set?
        if (args.length == 4) {
            try {
                // get classloader
                final ClassLoader classLoader = App.class.getClassLoader();

                // provision SSL context
                final String keystore = args[3];
                final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                final char[] keyPassPhrase = "password".toCharArray();
                final KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(classLoader.getResourceAsStream(keystore), keyPassPhrase);
                kmf.init(ks, keyPassPhrase);

                final KeyStore tks = KeyStore.getInstance("JKS");
                tks.load(classLoader.getResourceAsStream(keystore), null);
                final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(tks);

                final SSLContext sslContext = SSLContext.getInstance(DEFAULT_SSL_PROTOCOL);
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                // connect to secure server
                conn = builder.sslContext(sslContext).connect();
            } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        } else {
            try {
                // connect to insecure server
                conn = builder.connect();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        // was connection established?
        if (conn != null && conn.isOpen()) {
            // perform database creation, usage and deletion
            r.dbCreate("testDb").run(conn);
            conn.use("testDb");
            r.dbDrop("testDb").run(conn);

            // disconnect
            conn.close();
        }
    }

}
