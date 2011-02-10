import org.specs.Specification

object FirstSpec extends Specification {
  "foo" should {
    "bar" in {
      1 must_== 1
    }
  }
}
