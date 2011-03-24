package example
package snippet

import example.commands._
import net.liftweb.util._
import Helpers._
import java.util.UUID
import net.liftweb.sitemap._
import net.liftweb.http._
import org.squeryl.PrimitiveTypeMode._

object Products {
  import Loc._

  val menu = Menu(
    Loc("products", "products" :: "index" :: Nil, "Products", Stateless))

  def products = transaction { model.Product.findAll.toIndexedSeq }
}

class Products extends DispatchSnippet {
  def dispatch: DispatchIt = {
    case "list" => list
  }

  private def list = {
    ".products" #> Products.products.map(p =>
<tr>
<td>{p.title}</td>
<td>{p.description}</td>
<td>{p.imageUrl.getOrElse("")}</td>
<td>{p.price}</td>
<td><a href={"/products/" + p.id}>show</a></td>
<td><a href={"/products/" + p.id + "/edit"}>edit</a></td>
<td><a href={"/products/" + p.id + "/delete"} data-method="DELETE" data-confirm="Are you sure?">destroy</a></td>
</tr>)
  }
}
