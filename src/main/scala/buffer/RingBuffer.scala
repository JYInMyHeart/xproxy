package buffer

import org.jetbrains.annotations.{Contract, NotNull}

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
  private[this] var buffer: Array[Byte] = _

  /** read index */
  private[this] var readIndex: Int = 0

  /** write index */
  private[this] var writeIndex: Int = 0

  /** write index */
  private[this] var capacity: Int = _

  /** a flag demonstrate whether writeIndex after readIndex */
  private[this] var writeAfterRead: Boolean = false

  /** Creates a ring buffer with a given capacity.
    *
    *  @param capacity capacity
    *
    */
  def this(@NotNull capacity: Int) {
    this()
    this.capacity = capacity
    buffer = Array.ofDim(capacity)
  }

  /** Get a byte from ring buffer. */
  @Contract(pure = true)
  def getByte: Option[Byte] = {

    /** A helper method for geteing a byte from ring buffer. */
    @Contract(pure = true)
    def helper: Option[Byte] = {
      val returnByte = buffer(readIndex)
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
        Some(buffer(readIndex + index))
    }
  }
}
