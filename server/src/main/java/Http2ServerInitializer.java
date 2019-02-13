import static io.netty.handler.codec.http2.Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

class Http2ServerInitializer extends ChannelInitializer<SocketChannel> {

  private int maxHttpContentLength;
  private final SslContext sslContext;

  public Http2ServerInitializer(SslContext sslContext) {
    this.sslContext = sslContext;
    this.maxHttpContentLength = 8 * (2 << 10);
  }

  public Http2ServerInitializer(int maxHttpContentLength, SslContext sslContext) {
    if (maxHttpContentLength < 0) {
      throw new IllegalArgumentException("maxHttpContentLength should be > 0, but it is " + maxHttpContentLength);
    }

    this.sslContext = sslContext;
    this.maxHttpContentLength = maxHttpContentLength;
  }


  private static final UpgradeCodecFactory upgradeCodecFactory = new UpgradeCodecFactory() {

    public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
      if (AsciiString.contentEquals(HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
        return new Http2ServerUpgradeCodec(new HelloWorldHttp2HandlerBuilder().build());
      } else {
        return null;
      }
    }
  };

  // Main method of the initializer
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    if (sslContext != null) {
      // ALPN protocol negotiation will be used
      configureSslPipeline(ch);
    } else {
      // Upgrade header negotiation
      configureClearTextPipeline(ch);
    }
  }

  private void configureSslPipeline(SocketChannel ch) {
    ch.pipeline().addLast(sslContext.newHandler(ch.alloc()), new Http2OrHttpHandler());
  }

  private void configureClearTextPipeline(SocketChannel ch) {
    final HttpServerCodec sourceCodec = new HttpServerCodec();
    final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
    final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
        new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, new HelloWorldHttp2HandlerBuilder().build());

    final ChannelPipeline pipeline = ch.pipeline();

    pipeline.addLast(cleartextHttp2ServerUpgradeHandler);
    pipeline.addLast(new SimpleChannelInboundHandler<HttpMessage>() {

      protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
        // If this handler is reached, then no upgrade was attempted and the client is just talking http
        final ChannelPipeline pipeline1 = ctx.pipeline();
        ChannelHandlerContext ctx1 = pipeline1.context(this);
        pipeline1.addAfter(ctx1.name(), null, new HelloWorldHttp1Handler("Direct. No upgrade attempted."));
        pipeline1.replace(this, null, new HttpObjectAggregator(maxHttpContentLength));
        ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
      }
    });
    pipeline.addLast(new UserEventLogger());
  }

  private class UserEventLogger extends ChannelInboundHandlerAdapter {

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      System.out.println("User event triggered: " + evt);
      ctx.fireUserEventTriggered(evt);
    }
  }
}
