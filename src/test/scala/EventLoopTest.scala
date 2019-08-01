import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{
  SelectableChannel,
  SelectionKey,
  Selector,
  ServerSocketChannel,
  SocketChannel
}

import eventloop.EventLoop

class EventLoopTest extends UnitSpec {

  def prettyInput(buffer: ByteBuffer) = {
    buffer.flip()
    val tbyte = Array.ofDim[Byte](buffer.limit())
    buffer.get(tbyte)
    buffer.clear()
    new String(tbyte)
  }

  class ServerHandler(val selector: Selector) extends handler.Handler {
    override def channelRead(channel: SelectableChannel): Unit = {}

    override def channelWrite(channel: SelectableChannel): Unit = {}

    override def channelConnect(channel: SelectableChannel): Unit = {}

    override def channelAccept(channel: SelectableChannel): Unit = {
      var client: SocketChannel = null
      try {
        client = channel.asInstanceOf[ServerSocketChannel].accept()
        client.configureBlocking(false)
        client.register(selector,
                        SelectionKey.OP_READ,
                        new ClientHandler(selector))
        selector.wakeup
      } catch {
        case e: IOException =>
          println(e)
          client.close()
      }
    }

    override def exceptionCatch(channel: SelectableChannel): Unit = {}
  }

  class ClientHandler(val selector: Selector) extends handler.Handler {
    private val input: ByteBuffer = ByteBuffer.allocate(8)
    private val output: ByteBuffer = ByteBuffer.allocate(8)
    override def channelRead(channel: SelectableChannel): Unit = {
      val ch = channel.asInstanceOf[SocketChannel]
      try {
        ch.read(input)
        if (input.position() > 0) {
          println(prettyInput(input))
          ch.keyFor(selector).interestOps(SelectionKey.OP_WRITE)
        }
      } catch {
        case _: Exception =>
      }

    }

    override def channelWrite(channel: SelectableChannel): Unit = {
      val ch = channel.asInstanceOf[SocketChannel]

      ch.write(ByteBuffer.wrap("sb".getBytes()))
      ch.keyFor(selector).interestOps(SelectionKey.OP_READ)
    }

    override def channelConnect(channel: SelectableChannel): Unit = ???

    override def channelAccept(channel: SelectableChannel): Unit = ???

    override def exceptionCatch(channel: SelectableChannel): Unit = ???
  }

  class EpollClient {
    def run(msg: String) = {
      try {
        val socketChannel = SocketChannel.open()
        socketChannel.connect(new InetSocketAddress("localhost", 9999))

        val writeBuffer = ByteBuffer.allocate(32)
        val readBuffer = ByteBuffer.allocate(32)

        writeBuffer.put(msg.getBytes())
        writeBuffer.flip()

        for (i <- 0 until 1) {

          writeBuffer.rewind()
          socketChannel.write(writeBuffer)
          //  Thread.sleep(1000)
          readBuffer.clear()
          socketChannel.read(readBuffer)
          println(prettyInput(readBuffer))

        }

        def prettyInput(buffer: ByteBuffer) = {
          buffer.flip
          val tbyte = Array.ofDim[Byte](buffer.limit())
          buffer.get(tbyte)
          new String(tbyte)
        }

      } catch {
        case e: IOException =>
          e.printStackTrace()
      }
    }
  }

  it should "hello" in {
    val eventLoop = new EventLoop(9999)
    eventLoop.bind(9999, new ServerHandler(eventLoop.selector))
    eventLoop.run()

  }
}
