package eventloop

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel}
import java.util

import handler.Handler

import scala.collection.mutable

class EventLoop(val port: Int) extends Runnable {
  val selector: Selector = Selector.open()
  val serverSocketChannel: ServerSocketChannel = ServerSocketChannel.open()
  private val timeOut: Long = 1000

  override def run(): Unit = { loop() }

  def bind(port: Int, handler: Handler): Unit = {
    serverSocketChannel.socket().bind(new InetSocketAddress(port))
    serverSocketChannel.configureBlocking(false)
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, handler)
  }

  def doBeforeHandling(keys: mutable.Set[SelectionKey]): Unit = {}
  def doHandling(keys: mutable.Set[SelectionKey]): Unit = {
    keys.foreach(key => {
      val handler = key.attachment().asInstanceOf[Handler]
      key.readyOps() match {
        case SelectionKey.OP_ACCEPT  => handler.channelAccept(key.channel())
        case SelectionKey.OP_WRITE   => handler.channelWrite(key.channel())
        case SelectionKey.OP_READ    => handler.channelRead(key.channel())
        case SelectionKey.OP_CONNECT => handler.channelConnect(key.channel())
        case _                       => handler.exceptionCatch(key.channel())
      }
    })
  }
  def doAfterHandling(keys: mutable.Set[SelectionKey]): Unit = {
    keys.clear()
  }

  def loop(): Unit = {
    import scala.collection.JavaConverters._
    while (!Thread.interrupted()) {
      val keySize = selector.select(timeOut)
      if (keySize > 0) {
        val keys = selector.selectedKeys().asScala
        doBeforeHandling(keys)
        doHandling(keys)
        doAfterHandling(keys)
      }
    }
  }

}
