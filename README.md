[toc]
# netty学习
首先要明确的一点是： netty并未实现Servlet的相关接口。所以跟我们使用的tomcat的容器是不太一样的。<br/>
简单的来说，netty的编程主要分为三个步骤：
1. server端、client端
```java
public static void main(String[] args) {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MyServerInitializer());

        ChannelFuture channelFuture = serverBootstrap.bind(8899).sync();
        channelFuture.channel().closeFuture().sync();

    } catch (Exception e) {
        e.printStackTrace();
    }finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
```
2. 实现相关的ChannelInitializer继承`ChannelInitializer`；
```java
/**
 * 客户端与服务端建立连接后，这个方法就被调用
 * @param socketChannel
 * @throws Exception
 */
@Override
protected void initChannel(SocketChannel socketChannel) throws Exception {
    ChannelPipeline pipeline = socketChannel.pipeline();
    pipeline.addLast("LengthFieldBasedFrameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
    pipeline.addLast("LengthFieldPrepender", new LengthFieldPrepender(4));
    pipeline.addLast("StringDecoder", new StringDecoder(CharsetUtil.UTF_8));
    pipeline.addLast("StringEncoder", new StringEncoder(CharsetUtil.UTF_8));
    pipeline.addLast("MyClientHandler", new MyClientHandler());
}
```
3. 实现对应的handler继承`SimpleChannelInboundHandler`
```java
 /**
 *
 * @param channelHandlerContext
 * @param s 客户端发送过来的数据
 * @throws Exception
 */
@Override
protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
    System.out.println("客户端：" + channelHandlerContext.channel().remoteAddress() + " : " + s);
    channelHandlerContext.channel().writeAndFlush("get msg form server: " + UUID.randomUUID().toString());
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();

}
```
## 一个简单的`Hello World`程序
### 服务端编写
1. 声明两个EventLoopGroup;
    1. boss: 接收连接的进程
    2. worker: 处理接收到的连接
2. 绑定NioSocketServerClass;
3. 绑定 childChannel;
    1. channelInitializer;
    2. SimpleChannelInBoundHandler;
## 广播的实现
在服务端启动的时候，有新的客户端连接进的时候，服务端向其他的客户端发送：[xxx 加入]信息。
当客户端广播消息的时候，其他客户端接收消息显示[ xxx 发送]；自己显示：[自己发送]。<br/>
**实现这个需求的关键是知道以下几个知识点：**
1. 当新的客户端连接建立的时候，调用什么回调方法；<br/>
    `handlerAdded`。该方法是当有新的客户端连接服务端的时候，服务端触发的回调方法。
2. 服务端怎么保存所有连接进来的客户端连接（channel）； <br/>
    ```java
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    ```
   


*  `Initilatizer` <br/>

```java
 @Override
protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("DelimiterBasedFrameDecoder", new DelimiterBasedFrameDecoder(4096, Delimiters.lineDelimiter()));
    pipeline.addLast("StringDecoder", new StringDecoder(CharsetUtil.UTF_8));
    pipeline.addLast("StringEncoder", new StringEncoder(CharsetUtil.UTF_8));
    pipeline.addLast(new MyChatClientChannelHandler());

}
```

* `handler` <br/>

```java
 /**
 * 保存channel对象
 */
private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
private static final String DATE_PARTTEN = "yyyy-MM-dd HH:mm:ss:SSS";
@Override
protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    Channel channel = ctx.channel();
    channelGroup.forEach(ch -> {
        // 当前遍历的channel不是发送msg的channel对象。则向其他客户端广播
        if (channel != ch) {
            ch.writeAndFlush(channel.remoteAddress() + ", 发送的消息" + msg + "\n");
        } else {
            ch.writeAndFlush("[自己] " + msg + " \n");
        }
    });
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
}

@Override
public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    System.out.println(channel.remoteAddress() + " 上线了！");
}

@Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    System.out.println(channel.remoteAddress() + " 离开了！");

}

/**
 * 客户端链接建立的时候调用
 * @param ctx
 * @throws Exception
 */
@Override
public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    //super.handlerAdded(ctx);
    // 服务端与客户端建立
    Channel channel = ctx.channel();
    // 向其他链接的客户端发送广播信息
    SocketAddress socketAddress = channel.remoteAddress();
    String date = DateTimeFormatter.ofPattern(DATE_PARTTEN).format(LocalDateTime.now());
    // 向channelGroup中的每一个channel对象发送一个消息
    channelGroup.writeAndFlush(date + " [服务器] - " + socketAddress + " 加入 \n");
    // 保存该客户端链接
    channelGroup.add(channel);
}

/**
 * 链接断开
 * @param ctx
 * @throws Exception
 */
@Override
public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    String date = DateTimeFormatter.ofPattern(DATE_PARTTEN).format(LocalDateTime.now());

    channelGroup.writeAndFlush(date + " [服务器] - " + channel.remoteAddress() + " 离开 \n");
}
```


## 学习地址
[B站视频地址信息](https://www.bilibili.com/video/av33707223?p=4)
