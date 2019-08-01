package handler

import java.nio.channels.SelectableChannel

trait Handler {
  def channelRead(channel: SelectableChannel)

  def channelWrite(channel: SelectableChannel)

  def channelConnect(channel: SelectableChannel)

  def channelAccept(channel: SelectableChannel)

  def exceptionCatch(channel: SelectableChannel)
}
