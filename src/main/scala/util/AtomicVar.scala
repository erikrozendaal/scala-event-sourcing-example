package com.zilverline.es2
package util

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

class AtomicVar[A](initial: A) {
  def get:A = value.get

  @tailrec
  final def modifyWithRetry(f: A => A): A = {
    val previous = value.get
    val modified = f(previous)
    if (value.compareAndSet(previous, modified)) modified
    else modifyWithRetry(f)
  }

  private val value = new AtomicReference(initial)
}
