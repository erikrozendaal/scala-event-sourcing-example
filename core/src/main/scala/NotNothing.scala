package com.zilverline.es2

// NotNothing marker to avoid inferring nothing as valid type,
// which will cause runtime class cast errors.
// See: http://www.tikalk.com/java/blog/avoiding-nothing

sealed trait NotNothing[-T]

object NotNothing {
  implicit object notNothing extends NotNothing[Any]
  implicit object `The error is because the missing type parameter was resolved to Nothing` extends NotNothing[Nothing]
}
