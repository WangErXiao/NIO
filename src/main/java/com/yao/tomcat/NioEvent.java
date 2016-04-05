package com.yao.tomcat;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by yao on 16/4/5.
 */
public class NioEvent implements Runnable{

    private Selector selector;
    private SocketChannel socketChannel;
    private int intOps;


    public NioEvent(Selector selector, SocketChannel socketChannel, int intOps) {
        this.selector = selector;
        this.socketChannel = socketChannel;
        this.intOps = intOps;
    }

    @Override
    public void run() {
        try {
            socketChannel.configureBlocking(false);
            selector.wakeup();
            socketChannel.register(selector, intOps);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
