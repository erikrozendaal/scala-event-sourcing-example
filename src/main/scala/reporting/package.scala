package com.zilverline.es2

import com.zilverline.es2.eventing._

package object reporting {
  type Investigator[T <: Event] = T PartialFunction SpecificReport
}
