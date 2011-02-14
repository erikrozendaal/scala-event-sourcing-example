package com.zilverline.es2

import java.util.UUID

package object events {
  type Identifier = UUID

  def newIdentifier: Identifier = UUID.randomUUID
}
