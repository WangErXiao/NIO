package com.yao.tomcat;

import java.io.IOException;

/**
 * Created by yao on 16/4/4.
 */
public class Server {

    private Acceptor acceptor;
    private Poller poller;


    public Server() throws IOException {
        this.poller = new Poller();
        this.acceptor = new Acceptor(poller);
    }

    public void start() throws InterruptedException {
        Thread threadAcceptor= new Thread(acceptor);
        threadAcceptor.start();
        Thread threadPoller= new Thread(poller);
        threadPoller.start();
        threadAcceptor.join();

    }

    public static void main(String[]args) throws IOException, InterruptedException {
        Server server=new Server();
        server.start();
    }

}
