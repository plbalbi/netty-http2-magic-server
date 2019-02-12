import java.net.InetSocketAddress;

import handlers.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class EchoServer {

  private final int port;


  public EchoServer(int port) {
    this.port = port;
  }

  public static void main(String[] argv) throws InterruptedException {
    if (argv.length != 1) {
      System.out.println("Usage " + EchoServer.class.getSimpleName() + " <portNumber>");
      System.exit(1);
    }

    int port = Integer.parseInt(argv[0]);
    new EchoServer(port).start();
  }

  public void start() throws InterruptedException {
    final EchoServerHandler echoServerHandler = new EchoServerHandler();
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap
          .group(group)
          .channel(NioServerSocketChannel.class)
          .localAddress(new InetSocketAddress(port))
          .childHandler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(echoServerHandler);
            }
          });
      ChannelFuture future = bootstrap.bind().sync();
      System.out.println("Server connected on localhost:" + port);
      future.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully().sync();
    }
  }
}
