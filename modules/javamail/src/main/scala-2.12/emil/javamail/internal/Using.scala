package emil.javamail.internal

import scala.util.control.{ControlThrowable, NonFatal}

object Using {
  // Copy from Scala 2.13

  /** Performs an operation using a resource, and then releases the resource,
    * even if the operation throws an exception. This method behaves similarly
    * to Java's try-with-resources.
    *
    * @param resource the resource
    * @param body     the operation to perform with the resource
    * @tparam R the type of the resource
    * @tparam A the return type of the operation
    * @return the result of the operation, if neither the operation nor
    *         releasing the resource throws
    */
  def resource[R <: AutoCloseable, A](resource: R)(body: R => A): A = {
    if (resource == null) throw new NullPointerException("null resource")

    var toThrow: Throwable = null
    try {
      body(resource)
    } catch {
      case t: Throwable =>
        toThrow = t
        null.asInstanceOf[A] // compiler doesn't know `finally` will throw
    } finally {
      if (toThrow eq null) resource.close()
      else {
        try resource.close()
        catch {
          case other: Throwable => toThrow = preferentiallySuppress(toThrow, other)
        } finally throw toThrow
      }
    }
  }

  private def preferentiallySuppress(
      primary: Throwable,
      secondary: Throwable
  ): Throwable = {
    def score(t: Throwable): Int = t match {
      case _: VirtualMachineError                   => 4
      case _: LinkageError                          => 3
      case _: InterruptedException | _: ThreadDeath => 2
      case _: ControlThrowable                      => 0
      case e if !NonFatal(e)                        => 1 // in case this method gets out of sync with NonFatal
      case _                                        => -1
    }
    @inline def suppress(t: Throwable, suppressed: Throwable): Throwable = {
      t.addSuppressed(suppressed); t
    }

    if (score(secondary) > score(primary)) suppress(secondary, primary)
    else suppress(primary, secondary)
  }
}
