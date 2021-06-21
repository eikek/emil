package emil.javamail.internal

object Using {
  def resource[R <: AutoCloseable, A](resource: R)(body: R => A): A =
    scala.util.Using.resource(resource)(body)
}
