package com.zilverline.es2
package util

object AtomicVarSpec extends org.specs.Specification {
  val subject = new AtomicVar(1)

  "get current value" in {
    subject.get must beEqualTo(1)
  }

  "modify value" in {
    subject.modifyWithRetry(_ + 1) must beEqualTo(2)
    subject.get must beEqualTo(2)
  }
}
