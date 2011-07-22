package com.zilverline.es2
package util

import org.slf4j.{LoggerFactory, Logger}

trait Logging {
  protected[this] val logger: Logger = LoggerFactory.getLogger(getClass)

  protected[this] def profile[S](message: => String)(block: => S): S = {
    val start = System.currentTimeMillis
    try {
      block
    } finally {
      val stop = System.currentTimeMillis
      if (logger.isDebugEnabled)
        logger.debug("%s in %s milliseconds".format(message, stop - start))
    }
  }
}
