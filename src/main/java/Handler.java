/**
 * @Author: xck
 * @File: Handler
 * @Time: 17:28 2019/7/29
 */
public interface Handler extends ChannelEvent{

  boolean channelOpen();

  void channelRead();

  void channelWrite();

  void channelConnect();



}
