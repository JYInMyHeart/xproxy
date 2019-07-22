package eventloop

import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel}
import java.util

class SelectorEventLoop(var selector:Selector) {




  def loop():Unit = {
    while(true){
      val keySize = selector.select()
      keySize match {
        case 0 =>
        case x if x < 0 => throw new Exception("error")
        case _ =>
          val selectKeys = selector.selectedKeys().iterator()
          handling(selectKeys)
      }

    }
  }

  def handleBefore():Unit = {

  }

  def handleAfter():Unit = {

  }

  def connection(key: SelectionKey): Unit = {
    val server:ServerSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]
    val channel = server.accept()
    channel.configureBlocking(false)
    channel.register(selector,SelectionKey.OP_READ)
  }

  def readable(key: SelectionKey): Unit = {

  }

  def writable(key: SelectionKey): Unit = {

  }

  def handling(selectKeys: util.Iterator[SelectionKey]): Unit = {
    while(selectKeys.hasNext){
      selectKeys.remove()
      val key = selectKeys.next()
        key.readyOps match {
        case SelectionKey.OP_ACCEPT =>
          connection(key)
        case SelectionKey.OP_READ =>
            readable(key)
        case SelectionKey.OP_WRITE =>
            writable(key)
      }
    }
  }


}
