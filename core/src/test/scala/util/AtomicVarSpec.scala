package com.zilverline.es2
package util

class AtomicVarSpec extends org.specs2.mutable.SpecificationWithJUnit {

  "get current value" in {
    val subject = new AtomicVar(1)

    subject.get must beEqualTo(1)
  }

  "modify value" in {
    val subject = new AtomicVar(1)

    subject.modifyWithRetry(_ + 1) must beEqualTo(2)
    subject.get must beEqualTo(2)
  }
}
