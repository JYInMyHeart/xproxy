package buffer

import java.nio.ByteBuffer

class RingBuffer(val byteBuffer: ByteBuffer,
                 val capacity:Int){
  private[this] var startPos:Int = _
  private[this] var endPos:Int = _
  private[this] var startAfterEnd:Boolean = _

  def writeTo(): Unit ={

  }

  def storeLimit:Int = startAfterEnd match {
    case true => startPos - endPos
    case false => capacity - endPos
  }

  def retrieveLimit(sPos:Int,startAfterEnd:Boolean): Int = startAfterEnd match {
    case true => capacity - endPos
    case false => endPos - startPos
  }

  def retrieveLimit:Int = retrieveLimit(startPos,startAfterEnd)
}

object RingBuffer {
  def apply(byteBuffer: ByteBuffer, capacity: Int): RingBuffer = new RingBuffer(byteBuffer, capacity)
  def allocateDirect(cap:Int): RingBuffer = {
    apply(ByteBuffer.allocateDirect(cap),cap)
  }
}
