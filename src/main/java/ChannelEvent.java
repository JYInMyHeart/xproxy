import java.nio.channels.SelectableChannel;

/**
 * @Author: xck
 * @File: HandlerEvent
 * @Time: 22:45 2019/7/31
 */
public interface ChannelEvent {
  ChannelEventType getEventType();
  void execute(SelectableChannel channel);
  void modifyEventType(ChannelEventType channelEventType);

}
