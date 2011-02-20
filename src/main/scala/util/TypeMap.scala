package com.zilverline.es2
package util

import scala.collection.mutable.LinkedHashSet

class TypeMap[+A] private (underlying: Map[Class[_], A]) {
  def +[A1 >: A](kv: (Class[_], A1)): TypeMap[A1] = new TypeMap(underlying + kv)

  def get(key: Class[_]): Option[A] = underlying.get(key)

  def getMostSpecific(key: Class[_]): Option[A] = {
    underlying.get(key) orElse {
      val keys = LinkedHashSet.empty[Class[_]]
      if (key.getSuperclass != null)
        keys += key.getSuperclass
      keys ++= key.getInterfaces
      searchMostSpecific(keys)
    }
  }

  private def searchMostSpecific(keys: LinkedHashSet[Class[_]]): Option[A] = {
    while (keys.nonEmpty) {
      val key = keys.head
      keys.remove(key)
      val found = underlying.get(key)
      if (found.isDefined) return found
      if (key.getSuperclass != null)
        keys += key.getSuperclass
      keys ++= key.getInterfaces
    }
    None
  }
}

object TypeMap {
  def empty: TypeMap[Nothing] = new TypeMap[Nothing](Map.empty)
}
