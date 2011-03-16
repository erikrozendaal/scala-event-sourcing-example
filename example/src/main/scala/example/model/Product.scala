package example
package model

import org.squeryl._
import annotations.Column
import org.squeryl.PrimitiveTypeMode._

case class Product(
  id: Long,
  title: String,
  description: String,
  imageUrl: Option[String],
  @Column(length = 8, scale = 2)
  price: BigDecimal) extends KeyedEntity[Long] {
  def this() = this (-1, "", "", Some(""), 0)
}

object Product extends Schema {
  override def defaultLengthOfString = 255
  val products = table[Product]("products")

  on(products) {t =>
    declare(
      t.description is (dbType("text")))
  }

  def findAll = from(products)(p => select(p))

  def insertTestData(n: Int) {
    for (i <- 1 to n) {
      products.insert(Product(-1, "Product %d" format i, "Dit is product %d." format i, Some("/images/product%d.jpg" format i), 4.95))
    }
  }

  def deleteAll() {
    products.deleteWhere(_ => true)
  }
}
