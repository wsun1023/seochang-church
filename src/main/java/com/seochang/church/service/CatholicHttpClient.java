package com.seochang.church.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Locale;

/** HTTPS trust configuration scoped to the Catholic GoodNews client. */
@Component
public class CatholicHttpClient {
    private static final Logger log = LoggerFactory.getLogger(CatholicHttpClient.class);
    private final SSLSocketFactory socketFactory;

    public CatholicHttpClient(@Value("${catholic.http.use-windows-root:true}") boolean useWindowsRoot)
            throws GeneralSecurityException, IOException {
        socketFactory = createSocketFactory(useWindowsRoot);
    }

    public Connection connect(String url, int timeoutMillis) {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"maria.catholic.or.kr".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException("Only the Catholic GoodNews HTTPS endpoint is supported");
        }
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(timeoutMillis)
                .followRedirects(false)
                .sslSocketFactory(socketFactory);
    }

    static SSLSocketFactory createSocketFactory(boolean useWindowsRoot) throws GeneralSecurityException, IOException {
        TrustManagerFactory jvm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        jvm.init((KeyStore) null);
        var managers = jvm.getTrustManagers();
        if (useWindowsRoot && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            KeyStore roots = KeyStore.getInstance(KeyStore.getDefaultType());
            roots.load(null, null);
            int index = 0;
            for (var manager : managers) {
                if (manager instanceof X509TrustManager trusted) {
                    for (var certificate : trusted.getAcceptedIssuers()) {
                        roots.setCertificateEntry("jvm-" + index++, certificate);
                    }
                }
            }
            try {
                KeyStore windows = KeyStore.getInstance("Windows-ROOT");
                windows.load(null, null);
                var aliases = windows.aliases();
                int added = 0;
                while (aliases.hasMoreElements()) {
                    var certificate = windows.getCertificate(aliases.nextElement());
                    if (certificate != null) roots.setCertificateEntry("windows-" + added++, certificate);
                }
                TrustManagerFactory combined = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                combined.init(roots);
                managers = combined.getTrustManagers();
                log.info("Catholic HTTPS client uses JVM trust plus {} Windows root certificates", added);
            } catch (GeneralSecurityException | IOException e) {
                log.warn("Windows trust store unavailable; Catholic HTTPS client retains JVM certificate validation: {}", e.getMessage());
            }
        }
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, managers, null);
        return context.getSocketFactory();
    }
}
