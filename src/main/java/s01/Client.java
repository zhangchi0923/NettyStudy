package s01;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        //线程池，Event指的是网络事件
        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        try {
            ChannelFuture f = b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientChannelInitializer())
                    .connect("localhost",8887);//是异步方法，事实上netty中的所有方法都是异步的
            f.addListener(new ChannelFutureListener(){

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(f.isSuccess()) System.out.println("Connected!");
                    else System.out.println("Not connected!");
                }
            });
            f.sync();
            System.out.println("...");

            f.channel().closeFuture().sync();
                    /*.sync()*/;
        }finally {
            group.shutdownGracefully();
        }
    }
}
class ClientChannelInitializer extends ChannelInitializer<SocketChannel>{
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new ClientHandler());
    }
}

class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = null;
        try {
            buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(),bytes);
            System.out.println(new String(bytes));
//            System.out.println(buf);
//            System.out.println(buf.refCnt());
        }finally {
            if(buf != null) ReferenceCountUtil.release(buf);
//            System.out.println(buf.refCnt());
            List l;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //channel 第一次连上可用，写出一个字符串.ByteBuf使用的是直接访问内存，效率很高，但跳过了Java的垃圾回收机制
        // netty中所有写出的数据都要转成ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("dangdang".getBytes());
        ctx.writeAndFlush(buf);
    }
}