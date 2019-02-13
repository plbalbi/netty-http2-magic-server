import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler
{

    private static final int MAX_CONTENT_LENGTH = 100 * (2 << 10);

    public Http2OrHttpHandler()
    {
        super(HTTP_1_1);
    }

    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception
    {
        if (HTTP_2.equals(protocol)) {
            ctx.pipeline().addLast(new HelloWorldHttp2HandlerBuilder().build());
            return;
        }

        if (HTTP_1_1.equals(protocol)) {
            ctx.pipeline().addLast(new HttpServerCodec(),
                                   new HttpObjectAggregator(MAX_CONTENT_LENGTH),
                                   new HelloWorldHttp1Handler("ALPN Negotiation"));
            return;
        }

        throw new IllegalStateException("unknown protocol: " + protocol);

    }
}
