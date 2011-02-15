package com.zilverline.es2

package commands {
  import events._

  trait Command {
    val commandId = newIdentifier
  }
}
