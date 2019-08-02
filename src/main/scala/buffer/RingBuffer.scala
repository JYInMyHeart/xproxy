package buffer

import java.nio.ByteBuffer
import java.nio.channels.{ReadableByteChannel, WritableByteChannel}

import org.jetbrains.annotations.{Contract, NotNull}

import scala.collection.mutable

/** A ring buffer.
  *
  *  @constructor create a new ring buffer.
  *
  *  0                                     CAP
  *  first
  *  ....->[           free space           ]
  *  [sPos,ePos-----------------------------]
  *  pace       ]->           ->[      free s
  *  [---------sPos---------ePos------------]
  *  then
  *  [  free space  ]->                      ->
  *  [------------ePos--------------------sPos
  *  then
  *  .........->[   free space    ]->
  *  [------ePos----------------sPos--------]
  *  maybe we have
  *  ..........................->(cannot write into buf any more)
  *  [----------------------ePos,sPos--------]
  *
  */
class RingBuffer() {

  /** core byte array */
  private[this] var buffer: ByteBuffer = _

  /** read index */
  private[this] var readIndex: Int = 0

  /** write index */
  private[this] var writeIndex: Int = 0

  /** write index */
  private[this] var capacity: Int = _

  /** a flag demonstrate whether writeIndex after readIndex */
  private[this] var writeAfterRead: Boolean = false

  /** a flag demonstrate whether channel is operating */
  private[this] var operating = false

  /** a set collect some RingBufferETHandler which are operating */
  private[this] val handler = mutable.HashSet[RingBufferETHandler]()

  /** a set collect some RingBufferETHandler which are waiting for operating */
  private[this] val handlerToAdd = mutable.HashSet[RingBufferETHandler]()

  /** a set collect some RingBufferETHandler which are operated */
  private[this] val handlerToRemove = mutable.HashSet[RingBufferETHandler]()

  /** Creates a ring buffer with a given capacity.
    *
    *  @param capacity capacity
    *
    */
  def this(@NotNull capacity: Int) {
    this()
    this.capacity = capacity
    buffer = ByteBuffer.allocate(capacity)
  }

  /** Get a byte from ring buffer. */
  @Contract(pure = true)
  def getByte: Option[Byte] = {

    /** A helper method for getting a byte from ring buffer. */
    @Contract(pure = true)
    def helper: Option[Byte] = {
      val returnByte = buffer.get(readIndex)
      readIndex = (readIndex + 1) % capacity
      Some(returnByte)
    }

    if (writeAfterRead) {
      helper
    } else {
      if (readIndex + 1 > writeIndex) {
        None
      } else {
        helper
      }
    }
  }

  /** Get a byte from ring buffer by index. */
  @Contract(pure = true)
  def getByte(index: Int): Option[Byte] = {
    readIndex + index match {
      case x if x >= writeIndex || x < readIndex =>
        None
      case _ =>
        Some(buffer.get(readIndex + index))
    }
  }

  /** Get free space from ring buffer. */
  private[this] def storeLimit(): Int = {
    if (writeAfterRead) {
      readIndex - writeIndex
    } else {
      capacity - writeIndex
    }
  }

  /** Get used space from ring buffer. */
  private[this] def writeLimit(readPos: Int, writeLimit: Boolean): Int = {
    if (writeLimit) {
      writeIndex - readPos
    } else {
      capacity - readPos
    }
  }

  /** Get default used space from ring buffer. */
  private[this] def writeLimit(): Int = {
    writeLimit(this.readIndex, this.writeAfterRead)
  }

  /** Store bytes in ring buffer from channel. */
  def storeBytes(channel: ReadableByteChannel): Int = {
    operating = true
    var triggerReadable = false

    def readBuffer(storeLimit: Int): Either[Int, Int] = {
      buffer.limit(writeIndex + storeLimit).position(writeIndex)
      val read = channel.read(buffer)
      if (read < 0)
        return Left(read)
      writeIndex += read
      Right(read)
    }

    try {
      val usedSpace = used
      triggerReadable = usedSpace == 0 && handler.nonEmpty
      var storeLimit = this.storeLimit()
      if (storeLimit <= 0) return 0
      val read: Int = readBuffer(storeLimit) match {
        case Left(toReturn) => toReturn
        case Right(result)  => result
      }
      if (read < 0) return read

      triggerReadable = triggerReadable && read > 0

      if (writeIndex == capacity) {
        writeIndex = 0
        writeAfterRead = true
      }

      if (read == storeLimit) {
        storeLimit = this.storeLimit()
        if (storeLimit == 0)
          return read
        buffer.limit(writeIndex + read).position(writeIndex)
        val read2 = channel.read(buffer)
        if (read2 < 0)
          return read
        writeIndex += read2
        read + read2
      } else {
        read
      }
    } finally {
      if (triggerReadable) {
        handler.foreach(_.readET())
      }
      operating = false
      handler --= handlerToRemove
      handler ++= handlerToAdd
    }
  }

  /** Used space in ring buffer. */
  def used: Int = {
    if (writeAfterRead) {
      writeIndex + capacity - readIndex
    } else {
      writeIndex - readIndex
    }
  }

  /** Free space in ring buffer. */
  def free: Int = {
    capacity - used
  }

  /** Write bytes to ring buffer from channel. */
  def writeTo(channel: WritableByteChannel, maxWriteToBytes: Int): Int = {
    operating = true
    var triggerWritable = false

    def writeBuffer(realWrite: Int): Either[Int, Int] = {
      buffer.limit(readIndex + realWrite).position(readIndex)
      val write1 = channel.write(buffer)
      if (write1 < 0)
        return Left(write1)
      readIndex += write1
      Right(write1)
    }

    try {
      val free = this.free
      triggerWritable = free == 0 && handler.nonEmpty
      var lim = writeLimit()
      if (lim == 0) return 0
      var realWrite = math.min(maxWriteToBytes, lim)
      val write1: Int = writeBuffer(realWrite) match {
        case Left(toReturn) => return toReturn
        case Right(result)  => result
      }
      if (write1 < 0) return write1
      if (readIndex == capacity) {
        readIndex = 0
        writeAfterRead = false
      }
      triggerWritable = triggerWritable && write1 > 0

      if (write1 == lim && write1 < maxWriteToBytes) {
        lim = writeLimit()
        if (lim == 0) return write1
        realWrite = math.min(maxWriteToBytes - write1, lim)
        buffer.limit(readIndex + realWrite).position(readIndex)
        val write2 = channel.write(buffer)
        if (write2 < 0)
          return write1
        readIndex += write2
        write2 + write1
      } else {
        write1
      }
    } finally {
      if (triggerWritable) {
        handler.foreach(_.writeET())
      }
      operating = false
      handler --= handlerToRemove
      handler ++= handlerToAdd
    }
  }

}
