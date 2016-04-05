package com.yao.tomcat;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yao on 16/4/4.
 */
public class Poller implements Runnable {

    private Selector selector;
    private ExecutorService executorService;
    private ConcurrentLinkedQueue<NioEvent> nioEvents;

    private ConcurrentLinkedQueue<SocketProcessor>cacheProcessors;
    public Poller() throws IOException {
        this.selector=Selector.open();
        executorService= Executors.newFixedThreadPool(3);
        nioEvents=new ConcurrentLinkedQueue<NioEvent>();
        cacheProcessors=new ConcurrentLinkedQueue<SocketProcessor>();
    }

    @Override
    public void run() {

        while (true){

            try {
                Runnable event=null;
                while ((event=(Runnable)nioEvents.poll())!=null){
                    event.run();
                }
                selector.select(1000);
                Set<SelectionKey> keySet=selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keySet.iterator();
                while (keyIterator.hasNext()){
                    SelectionKey selectionKey=  keyIterator.next();
                    selectionKey.interestOps(selectionKey.interestOps() & (~selectionKey.readyOps()));
                    keyIterator.remove();
                    SocketProcessor socketProcessor= cacheProcessors.poll();
                    if (socketProcessor!=null){
                        socketProcessor.reset(selectionKey, selector);
                    }else{
                        socketProcessor=new SocketProcessor(selectionKey,selector);
                    }
                    executorService.submit(socketProcessor);

                }
                keySet.clear();
            } catch (IOException e) {

                e.printStackTrace();
            }

        }

    }

    public void register(SocketChannel socketChannel, int opRead) {
        NioEvent nioEvent=new NioEvent(selector,socketChannel,opRead);
        nioEvents.offer(nioEvent);
    }

    protected class SocketProcessor implements Runnable{

        private SocketChannel socketChannel;
        private SelectionKey selectionKey;
        private Selector selector;

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        public SocketProcessor(SelectionKey selectionKey,Selector selector) {
            this.selectionKey = selectionKey;
            this.socketChannel = (SocketChannel)selectionKey.channel();
            this.selector=selector;
        }

        public void reset(SelectionKey selectionKey,Selector selector){
            this.selectionKey = selectionKey;
            this.socketChannel = (SocketChannel)selectionKey.channel();
            this.selector=selector;
            buffer.clear();

        }

        @Override
        public void run() {

            try {
                boolean isClose=false;
                while (socketChannel.read(buffer)>0){
                    buffer.flip();
                    byte[] bytes = new byte[buffer.limit() - buffer.position()];
                    buffer.get(bytes, buffer.position(), buffer.limit() - buffer.position());
                    //ctrl c
                    if (bytes.length==3&&(bytes[0]=='q'||bytes[0]=='Q')){
                        System.out.println(socketChannel.socket().toString() + " end-----\n");
                        selectionKey.cancel();
                        socketChannel.close();
                        isClose=true;
                        break;
                    }
                    System.out.println("server:" + new String(bytes));
                    buffer.clear();

                }
                if (!isClose) {
                    buffer.clear();
                    buffer.put(("received msg!!! " + socketChannel.socket().toString() + "\n").getBytes());
                    buffer.flip();
                    socketChannel.write(buffer);
                    selector.wakeup();
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
                }

            } catch (IOException e) {
                e.printStackTrace();
                selectionKey.cancel();
                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } finally {
                selector.wakeup();
                selectionKey.interestOps(SelectionKey.OP_READ);
                cacheProcessors.offer(this);
            }

        }
    }
}

