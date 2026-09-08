package com.seochang.church.service;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.net.ssl.*;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import static org.assertj.core.api.Assertions.*;

class CatholicHttpClientTests {
    @TempDir static Path temporary;
    static HttpsServer server;

    @BeforeAll static void startUntrustedServer() throws Exception {
        Path storeFile = temporary.resolve("untrusted.p12");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        Process generator = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12",
                "-keystore", storeFile.toString(), "-storepass", "test-password", "-dname", "CN=localhost",
                "-ext", "SAN=dns:localhost", "-validity", "2", "-noprompt")
                .redirectErrorStream(true).redirectOutput(temporary.resolve("keytool.log").toFile()).start();
        assertThat(generator.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(generator.exitValue()).isZero();
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(storeFile)) { store.load(input, "test-password".toCharArray()); }
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, "test-password".toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), null, null);
        server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context));
        server.createContext("/", exchange -> { exchange.sendResponseHeaders(200, -1); exchange.close(); });
        server.start();
    }
    @AfterAll static void stopServer() { if (server != null) server.stop(0); }

    @Test void rejectsUntrustedCertificatesWithJvmTrust() throws Exception { assertUntrustedRejected(false); }
    @Test void rejectsUntrustedCertificatesWithWindowsTrustEnabled() throws Exception { assertUntrustedRejected(true); }
    private void assertUntrustedRejected(boolean windows) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + server.getAddress().getPort() + "/").openConnection();
        connection.setSSLSocketFactory(CatholicHttpClient.createSocketFactory(windows));
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try { assertThatThrownBy(connection::getResponseCode).isInstanceOf(SSLHandshakeException.class); }
        finally { connection.disconnect(); }
    }
    @Test void limitsCustomTrustToCatholicHttpsEndpoint() throws Exception {
        var client = new CatholicHttpClient(false);
        assertThatThrownBy(() -> client.connect("http://maria.catholic.or.kr/", 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.connect("https://other.example/", 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.connect("https://user@maria.catholic.or.kr/", 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.connect("https://maria.catholic.or.kr:8443/", 1000)).isInstanceOf(IllegalArgumentException.class);
        var request = client.connect("https://maria.catholic.or.kr/", 3000).request();
        assertThat(request.followRedirects()).isFalse();
        assertThat(request.sslSocketFactory()).isNotNull();
    }
    @Test void leavesGlobalTlsDefaultsUnchanged() throws Exception {
        var originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        var originalVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        new CatholicHttpClient(true);
        assertThat(HttpsURLConnection.getDefaultSSLSocketFactory()).isSameAs(originalFactory);
        assertThat(HttpsURLConnection.getDefaultHostnameVerifier()).isSameAs(originalVerifier);
    }
    @Test
    @EnabledIfEnvironmentVariable(named = "CATHOLIC_LIVE_TEST", matches = "true")
    void liveMassAndBibleRequestsSucceedWithVerifiedTls() throws Exception {
        var client = new CatholicHttpClient(true);
        var mass = new DailyMissaService(client).getDailyMissa("2026-09-08");
        assertThat(mass.getReadings()).isNotEmpty();
        assertThat(mass.getReadings().stream().map(r -> r.getType())).doesNotContain("오류", "안내");
        var verses = new BibleService(client).getBibleChapter(1, 1, 1);
        assertThat(verses).isNotEmpty();
    }
}
