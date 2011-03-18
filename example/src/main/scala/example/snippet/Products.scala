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
}

class Products extends DispatchSnippet {
  def dispatch: DispatchIt = {
    case "list" => list
  }

  private def list = transaction {
    ".products" #> model.Product.findAll.map(p =>
      <tr>
        <td>
          {p.title}
        </td> <td>
        {p.description}
      </td> <td>
        {p.imageUrl.getOrElse("")}
      </td> <td>
        {p.price}
      </td> <td></td> <td></td> <td></td>
      </tr>)
  }
}
