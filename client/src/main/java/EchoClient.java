import java.net.InetSocketAddress;

import handlers.EchoClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class EchoClient {

  private final int port;
  private final String host;

  public EchoClient(String host, int port) {
    this.port = port;
    this.host = host;
  }

  public static void main(String[] args) throws InterruptedException {
    if (args.length != 2) {
      System.out.println("Usage: " + EchoClient.class.getSimpleName() + " <host> <port>");
      System.exit(1);
    }

    new EchoClient(args[0], Integer.parseInt(args[1])).start();

  }

  public void start() throws InterruptedException {
    EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap
          .group(eventLoopGroup)
          .channel(NioSocketChannel.class)
          .remoteAddress(new InetSocketAddress(host, port))
          .handler(new ChannelInitializer<SocketChannel>() {

            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(new EchoClientHandler());
            }
          });
      ChannelFuture future = bootstrap.connect().sync();
      future.channel().closeFuture().sync();

    } finally {
      eventLoopGroup.shutdownGracefully().sync();
    }
  }
}
