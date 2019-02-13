import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeEvent;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.CharsetUtil;

public class HelloWorldHttp2Handler extends Http2ConnectionHandler implements Http2FrameListener
{

    public static final ByteBuf RESPONSE_BYTES = unreleasableBuffer(copiedBuffer("Hello bitch!", CharsetUtil.UTF_8));

    protected HelloWorldHttp2Handler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings)
    {
        super(decoder, encoder, initialSettings);
    }

    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception
    {
        int processed = data.readableBytes() + padding;
        if (endOfStream)
        {
            sendResponse(ctx, streamId, data.retain());
        }

        return processed;
    }

    private void sendResponse(ChannelHandlerContext ctx, int streamId, ByteBuf payload)
    {
        Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
        encoder().writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
        encoder().writeData(ctx,streamId, payload, 0, true, ctx.newPromise());

        // channelReadComplete will call flush
    }

    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception
    {
        if (endOfStream) {
            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(RESPONSE_BYTES.duplicate());
            ByteBufUtil.writeAscii(content, " - via HTTP2");
            sendResponse(ctx, streamId, content);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof UpgradeEvent) {
            UpgradeEvent upgradeEvent = (UpgradeEvent) evt;
            onHeadersRead(ctx, 1, http1HeadersToHttp2Headers(upgradeEvent.upgradeRequest()), 0, true);
        }

        super.userEventTriggered(ctx, evt);
    }

    private static Http2Headers http1HeadersToHttp2Headers(HttpRequest request)
    {
        CharSequence host = request.headers().get(HOST);
        Http2Headers http2Headers = new DefaultHttp2Headers()
                .method(HttpMethod.GET.asciiName())
                .path(request.uri())
                .scheme(HttpScheme.HTTP.name());

        if (host != null) {
            http2Headers.authority(host);
        }

        return http2Headers;
    }

    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception
    {
        onHeadersRead(ctx, streamId, headers, padding, endOfStream);
    }

    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception
    {

    }

    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception
    {

    }

    public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception
    {

    }

    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception
    {

    }

    public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception
    {

    }

    public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception
    {

    }

    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception
    {

    }

    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception
    {

    }

    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception
    {

    }

    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) throws Http2Exception
    {

    }
}
