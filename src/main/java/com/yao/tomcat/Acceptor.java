package com.yao.tomcat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by yao on 16/4/4.
 */
public class Acceptor implements Runnable {

    private ServerSocketChannel serverSocketChannel;
    private Poller poller;
    public Acceptor(Poller poller,int port) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.poller=poller;
        serverSocketChannel.bind(new InetSocketAddress(port));
    }
    public Acceptor(Poller poller) throws IOException {
        this(poller,9999);
    }


    @Override
    public void run() {
        try {
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                poller.register(socketChannel, SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
