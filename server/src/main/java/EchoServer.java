import static io.netty.handler.ssl.ApplicationProtocolConfig.Protocol.ALPN;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;

// Inspired by https://github.com/netty/netty/blob/4.1/example/src/main/java/io/netty/example/http2/helloworld/server/HelloWorldHttp1Handler.java

public class EchoServer {

  private final int port;

  private static final boolean SSL = System.getProperty("ssl") != null;

  private static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));
  private EventLoopGroup eventLoopGroup;
  private ChannelFuture serverChannelFuture;

  public EchoServer(int port) {
    this.port = port;
  }

  public static void main(String[] argv) throws InterruptedException, CertificateException, SSLException {
    new EchoServer(PORT).start();
  }

  private static SslContext configureSslContext() throws CertificateException, SSLException {
    // Configure an SSL context
    final SslContext sslContext;

    if (SSL) {
      SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
      SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
      sslContext = SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
          .sslProvider(provider)
          .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
          .applicationProtocolConfig(new ApplicationProtocolConfig(
                                                                   ALPN,
                                                                   NO_ADVERTISE,
                                                                   ACCEPT,
                                                                   HTTP_2,
                                                                   HTTP_1_1))
          .build();
    } else {
      sslContext = null;
    }
    return sslContext;
  }

  public void start() throws InterruptedException, SSLException, CertificateException {
    start(true);
  }

  public void start(boolean block) throws InterruptedException, CertificateException, SSLException {
    final SslContext sslContext = configureSslContext();

    eventLoopGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();

      bootstrap
          .group(eventLoopGroup)
          .option(ChannelOption.SO_BACKLOG, 1024)
          .channel(NioServerSocketChannel.class)
          .localAddress(new InetSocketAddress(port))
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(new Http2ServerInitializer(sslContext));

      serverChannelFuture = bootstrap.bind().sync();

      System.out.println("Server connected on localhost:" + port);

      if (block) {
        serverChannelFuture.channel().closeFuture().sync();
      }

    } finally {
      eventLoopGroup.shutdownGracefully().sync();
    }
  }

  public void shutdown() {
    eventLoopGroup.shutdownGracefully();
    try {
      serverChannelFuture.channel().closeFuture().sync();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }

  }

}
