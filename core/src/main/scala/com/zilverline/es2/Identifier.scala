package com.zilverline.es2

import java.lang.{Long => JLong}
import java.security.SecureRandom

/**
 * Simplified UUID implementation that saves 32 bytes per instance compared to [[java.util.UUID]].
 */
final class Identifier(val mostSigBits: Long, val leastSigBits: Long) extends Ordered[Identifier] {
  def compare(that: Identifier): Int = {
    val rc = this.mostSigBits compare that.mostSigBits
    if (rc != 0) rc
    else this.leastSigBits compare that.leastSigBits
  }

  override def equals(other: Any): Boolean = other match {
    case that: Identifier => this.mostSigBits == that.mostSigBits && this.leastSigBits == that.leastSigBits
    case _ => false
  }

  override def hashCode(): Int = mostSigBits.hashCode ^ leastSigBits.hashCode

  override def toString: String = (digits(mostSigBits >> 32, 8) + "-" +
    digits(mostSigBits >> 16, 4) + "-" +
    digits(mostSigBits, 4) + "-" +
    digits(leastSigBits >> 48, 4) + "-" +
    digits(leastSigBits, 12))

  private def digits(value: Long, digits: Int) = {
    val hi: Long = 1L << (digits * 4);
    JLong.toHexString(hi | (value & (hi - 1))).substring(1);
  }
}

object Identifier {
  def apply(): Identifier = {
    val randomBytes = new Array[Byte](16)
    random.nextBytes(randomBytes)
    randomBytes(6) = (randomBytes(6) & 0x0f).asInstanceOf[Byte]
    randomBytes(6) = (randomBytes(6) | 0x40).asInstanceOf[Byte]
    randomBytes(8) = (randomBytes(8) & 0x3f).asInstanceOf[Byte]
    randomBytes(8) = (randomBytes(8) | 0x80).asInstanceOf[Byte]

    var mostSigBits: Long = 0
    var leastSigBits: Long = 0
    for (i <- 0 to 7) mostSigBits = (mostSigBits << 8) | (randomBytes(i) & 0xff)
    for (i <- 8 to 15) leastSigBits = (leastSigBits << 8) | (randomBytes(i) & 0xff)

    new Identifier(mostSigBits, leastSigBits)
  }

  def fromString(name: String): Identifier = {
    val components = name.split("-")
    if (components.length != 5)
      throw new IllegalArgumentException("Invalid UUID string: " + name)

    for (i <- 0 to 4) components(i) = "0x" + components(i)

    var mostSigBits = JLong.decode(components(0)).longValue
    mostSigBits <<= 16
    mostSigBits |= JLong.decode(components(1)).longValue
    mostSigBits <<= 16
    mostSigBits |= JLong.decode(components(2)).longValue

    var leastSigBits = JLong.decode(components(3)).longValue
    leastSigBits <<= 48
    leastSigBits |= JLong.decode(components(4)).longValue

    return new Identifier(mostSigBits, leastSigBits);
  }

  private lazy val random = new SecureRandom()
}
