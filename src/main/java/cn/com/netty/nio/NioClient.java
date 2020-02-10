package cn.com.netty.nio;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @desc: cn.com.netty.nio.NioClient
 * @author: niejian9001@163.com
 * @date: 2020/2/10 21:14
 */
public class NioClient {

    public static void main(String[] args) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 8899));

            while (true) {
                selector.select();

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    if (selectionKey.isConnectable()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();

                        // 是否处于连接过程中
                        if (client.isConnectionPending()) {
                            client.finishConnect();

                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                            byteBuffer.put((LocalDateTime.now() + "连接成功").getBytes());
                            byteBuffer.flip();
                            client.write(byteBuffer);

                            ExecutorService executorService = Executors.newFixedThreadPool(10);
                            executorService.submit(() -> {
                                while (true) {
                                    byteBuffer.clear();
                                    InputStreamReader input = new InputStreamReader(System.in);
                                    BufferedReader br = new BufferedReader(input);
                                    String sendMessage = br.readLine();
                                    byteBuffer.put(sendMessage.getBytes());
                                    byteBuffer.flip();
                                    client.write(byteBuffer);
                                }
                            });
                        }

                    }
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
