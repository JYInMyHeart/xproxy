import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: xck
 * @File: Reactor
 * @Time: 11:03 2019/7/26
 */
public class Reactor implements Runnable {

  private Selector selector;
  private ServerSocketChannel serverSocketChannel;
  private int port;

  public Selector getSelector() {
    return selector;
  }

  public Reactor(int port) {
    this.port = port;
    try {
      selector = Selector.open();
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.socket().bind(new InetSocketAddress(port));
      register(serverSocketChannel, SelectionKey.OP_ACCEPT,
          new ServerHandler(serverSocketChannel, this));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void register(SelectableChannel channel, int ops, Object att)
      throws IOException {
    channel.configureBlocking(false);
    channel.register(selector, ops, att);
  }


  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        int selectSize = selector.select(2000);
        if (selectSize <= 0) {
          continue;
        }
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectionKeys.iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();
          dispatch(key);
        }
        selectionKeys.clear();

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void dispatch(SelectionKey key) {
    if(key.isValid()){
      Handler handler = (Handler) key.attachment();
      if (handler != null) {
        switch (handler.getEventType()){
          case READING:
            handler.channelRead();
            break;
          case SENDING:
            handler.channelWrite();
            break;
          case ACCEPTING:
            handler.channelConnect();
            break;
          case PROCESSING:
            break;
          default:
            throw new RuntimeException("unknown channel event type.");
        }
      }
    }
  }
}
