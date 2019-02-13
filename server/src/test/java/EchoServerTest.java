import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EchoServerTest
{

    private int port;
    private EchoServer server;

    @Test
    public void testSimpleRequest() throws Exception
    {
        HTTP2Client http2Client = new HTTP2Client();
        SslContextFactory sslContextFactory = new SslContextFactory();
        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP2(http2Client), sslContextFactory);
        httpClient.start();

        ContentResponse response = httpClient.GET("http://localhost:" + port);

        assertEquals(response.getContentAsString(), "perro");
    }

    @AfterEach
    void tearDown()
    {
        server.shutdown();
    }

    @BeforeEach
    void setUp() throws InterruptedException, SSLException, CertificateException
    {
        port = 8765;
        server = new EchoServer(port);
        // will block until server has binded to port
        server.start(false);
    }
}