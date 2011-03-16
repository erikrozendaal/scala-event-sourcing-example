package example.scalatra

import org.scalatra._
import example.app.Application._
import example.model.Product
import org.squeryl.PrimitiveTypeMode._
import xml.NodeSeq

object ScalatraProducts {
  @volatile var products = IndexedSeq[Product]()
  //def products = transaction { Product.findAll.toIndexedSeq }
}
class ScalatraProducts extends ScalatraFilter with CSRFTokenSupport with UrlSupport {

  eventStore

  protected def contextPath = request.getContextPath

  def standardLayout(body: => NodeSeq) = {
<html>
<head>
  <meta name="foo" content={"just some dummy data" + ("x" * 400)}/>
  <title>Demo</title>
  <link href="/stylesheets/scaffold.css?1291743272" media="screen" rel="stylesheet" type="text/css" />
  <script src="/javascripts/prototype.js?1291743132" type="text/javascript"></script>
<script src="/javascripts/effects.js?1291743132" type="text/javascript"></script>
<script src="/javascripts/dragdrop.js?1291743132" type="text/javascript"></script>
<script src="/javascripts/controls.js?1291743132" type="text/javascript"></script>
<script src="/javascripts/rails.js?1291743132" type="text/javascript"></script>
<script src="/javascripts/application.js?1291743132" type="text/javascript"></script>
<meta name="csrf-param" content={csrfKey}/>
<meta name="csrf-token" content={csrfToken}/>
</head>
<body>
{body}
</body>
</html>
  }

  get("/s/reset/:count") {
    transaction {
      Product.deleteAll
      Product.insertTestData(params('count).toInt)
    }
    transaction { ScalatraProducts.products = Product.findAll.toIndexedSeq }
    <p>Changed to {params('count)} products</p>
  }

  get("/s/products") { standardLayout {
<h1>Listing products</h1>

<table>
  <tr>
    <th>Title</th>
    <th>Description</th>
    <th>Image url</th>
    <th>Price</th>
    <th></th>
    <th></th>
    <th></th>
  </tr>

{ScalatraProducts.products map { product =>
  <tr>
    <td>{product.title}</td>
    <td>{product.description}</td>
    <td>{product.imageUrl.getOrElse("")}</td>
    <td>{product.price}</td>
    <td><a href={url("/products/" + product.id)}>Show</a></td>
    <td><a href={url("/products/" + product.id + "/edit")}>Edit</a></td>
    <td><a href={url("/products/" + product.id)} data-confirm="Are you sure?" data-method="delete" rel="nofollow">Destroy</a></td>
  </tr>
}}
</table>

<br />

<a href={url("/s/products/new")}>New Product</a>
  }}
}
