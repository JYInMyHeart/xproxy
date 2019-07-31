import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * @Author: xck
 * @File: Processer
 * @Time: 13:52 2019/7/26
 */
public class Processer implements Runnable {

  private ClientHandler handler;
  private Reactor reactor;

  @Override
  public void run() {
    processAndHandOff();
  }


  public Processer(ClientHandler handler, Reactor reactor) {
    this.handler = handler;
    this.reactor = reactor;
  }

  public Processer(ClientHandler handler) {
    this.handler = handler;
  }

  public void process() {

    try {
      byte[] tbyte = prettyInput(handler.input);
      System.out.println(new String(tbyte));
      handler.input.clear();
    } catch (Exception e) {
      e.printStackTrace();
    }


  }

  private byte[] prettyInput(ByteBuffer buffer) {
    buffer.flip();
    byte[] tbyte = new byte[buffer.limit()];
    buffer.get(tbyte);
    return tbyte;
  }

  synchronized void processAndHandOff() {
    process();
    handler.setState(ChannelEventType.SENDING);
    handler.getSc().keyFor(reactor.getSelector()).interestOps(SelectionKey.OP_WRITE);
  }
}
