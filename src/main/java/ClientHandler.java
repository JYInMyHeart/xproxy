import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: xck
 * @File: Handler
 * @Time: 11:25 2019/7/26
 */
public class ClientHandler implements Handler {

  private Reactor reactor;
  private final SocketChannel sc;
  private ChannelEventType state = ChannelEventType.READING;
  private int inSize = 8;
  private int outSize = 8;
  ByteBuffer input = ByteBuffer.allocate(inSize);
  private ByteBuffer output = ByteBuffer.allocate(outSize);
  static ExecutorService pool = Executors.newFixedThreadPool(4);

  public SocketChannel getSc() {
    return sc;
  }


  public void setState(ChannelEventType state) {
    this.state = state;
  }

  public ClientHandler(SocketChannel sc, Selector selector, Reactor reactor) {
    this.sc = sc;
    try {
      this.reactor = reactor;
      reactor.register(sc, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
      selector.wakeup();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean inputComplete() {
    return input.position() > 0;
  }

  public boolean outputComplete() {
    return true;
  }


  @Override
  public boolean channelOpen() {
   return false;
  }

  @Override
  public synchronized void channelRead() {
    try {
      Thread.sleep(100);
      sc.read(input);

      if (inputComplete()) {
        state = ChannelEventType.PROCESSING;
        pool.execute(new Processer(this, reactor));
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());

      try {
        sc.close();
      } catch (Exception ex) {

      }

    }
  }

  @Override
  public void channelWrite() {
    try {
      output = ByteBuffer.wrap("sb".getBytes());
      sc.write(output);
      if (outputComplete()) {
        state = ChannelEventType.READING;
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
      try {
        sc.close();
      } catch (Exception ex) {
      }
    }
  }

  @Override
  public void channelConnect() {

  }


  @Override
  public ChannelEventType getEventType() {
    return state;
  }

  @Override
  public void execute(SelectableChannel channel) {

  }

  @Override
  public void modifyEventType(ChannelEventType channelEventType) {

  }
}
