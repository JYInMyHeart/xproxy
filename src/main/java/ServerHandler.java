import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @Author: xck
 * @File: Acceptor
 * @Time: 11:14 2019/7/26
 */
public class ServerHandler implements Handler{

  private ServerSocketChannel serverSocketChannel;
  private Selector selector;
  private Reactor reactor;
  private ChannelEventType channelEventType;


  public ServerHandler(ServerSocketChannel serverSocketChannel, Reactor reactor) {
    this.serverSocketChannel = serverSocketChannel;
    this.selector = reactor.getSelector();
    this.reactor = reactor;
    this.channelEventType = ChannelEventType.ACCEPTING;
  }


  @Override
  public boolean channelOpen() {
    return false;
  }

  @Override
  public void channelRead() {
    channelEventType = ChannelEventType.ACCEPTING;
  }

  @Override
  public void channelWrite() {

  }

  @Override
  public void channelConnect() {
    try {
      SocketChannel socketChannel = serverSocketChannel.accept();
      if (socketChannel != null) {
        new ClientHandler(socketChannel, selector, reactor);
        channelEventType = ChannelEventType.READING;
      }
    } catch (IOException e) {
      e.printStackTrace();

    }
  }

  @Override
  public ChannelEventType getEventType() {
    return channelEventType ;
  }

  @Override
  public void execute(SelectableChannel channel) {

  }

  @Override
  public void modifyEventType(ChannelEventType channelEventType) {

  }
}
