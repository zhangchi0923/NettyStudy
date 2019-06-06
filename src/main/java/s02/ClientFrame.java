package s02;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientFrame extends Frame {
    static String headerToPrint = "Chatting Room";
    static TextArea ta = new TextArea(headerToPrint);
    static TextField tf = new TextField();

    public ClientFrame(){
        this.setSize(600,400);
        this.setLocation(100,20);
        this.setResizable(false);
        this.add(ta,BorderLayout.CENTER);
        this.add(tf,BorderLayout.SOUTH);
        tf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ta.setText(ta.getText()+"\n"+tf.getText());
                tf.setText("");
            }
        });
        this.setVisible(true);
        addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        new ClientFrame();

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();

        try{
            ChannelFuture f = b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientChannelInitializer2())
                    .connect("127.0.0.1",8889);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(f.isSuccess()) ClientFrame.headerToPrint = "Connected to Chat Room!";
                    else if(!f.isSuccess()) ClientFrame.headerToPrint = "Failed to connect...";
                }
            });
            f.sync();
//            System.out.println("......");
            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }
}
class ClientChannelInitializer2 extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new ClientHandler2());
    }
}
class ClientHandler2 extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String msgToSend = "has entered this room.";
        ByteBuf buf = Unpooled.copiedBuffer((Thread.currentThread().getName()+": "+msgToSend).getBytes());
        ctx.writeAndFlush(buf);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = null;
        try{
            buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(),bytes);
//            ctx.writeAndFlush(msg); 发给服务端后服务端又发给了自己，自己读到后又发回服务端，死循环
            ClientFrame.ta.setText(new String(bytes)+"\n"+ClientFrame.tf.getText());
        }finally{}
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
// 为啥新起一个客户端会导致已经开启的客户端重链接？？