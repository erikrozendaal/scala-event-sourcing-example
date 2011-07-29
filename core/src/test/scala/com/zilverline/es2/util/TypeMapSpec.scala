package com.zilverline.es2
package util

@org.junit.runner.RunWith(classOf[org.specs2.runner.JUnitRunner])
class TypeMapSpec extends org.specs2.mutable.Specification {
  val empty = TypeMap.empty

  trait Root
  trait Child1 extends Root
  trait Child2 extends Root
  class RootChild extends Root
  class Multiparent extends Child1 with Child2

  "allow adding new values indexed by class" in {
    val one = empty + (classOf[ExampleEvent], ExampleEvent("example"))

    one.get(classOf[ExampleEvent]) must beEqualTo(Some(ExampleEvent("example")))
  }

  "find value by most specific class" in {
    val s = empty + (classOf[Child1], "child1 value")

    s.getMostSpecific(classOf[Child1]) must beEqualTo(Some("child1 value"))
    s.getMostSpecific(classOf[Child2]) must beEqualTo(None)
    s.getMostSpecific(classOf[Multiparent]) must beEqualTo(Some("child1 value"))

    val s2 = s + (classOf[Child2], "child2 value")
    s2.getMostSpecific(classOf[Child2]) must beEqualTo(Some("child2 value"))
    s2.getMostSpecific(classOf[Multiparent]) must beEqualTo(Some("child1 value"))
  }
}
