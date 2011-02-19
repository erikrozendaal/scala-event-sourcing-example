package com.zilverline.es2
package reports

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

trait Index extends EventProcessor[DomainEvent, Index]

class AtomicHolder[A](initial: A) {
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

class Indexes {
  def add(index: Index) {
    indexByType.put(index.getClass, new AtomicHolder(index))
  }

  def get[T <: Index](implicit m: Manifest[T], nn: NotNothing[T]): T = indexByType(m.erasure).get.asInstanceOf[T]

  def process(event: CommittedEvent) {
    indexByType.values.foreach { index => index.modifyWithRetry(_.applyEvent(event)) }
  }

  private val indexByType: MMap[Class[_], AtomicHolder[Index]] = MMap.empty
}
