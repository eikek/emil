package emil

/** This trait is meant to be implemented by a concrete connection
  * implementation.
  *
  * User code can define type bounds on their mail operations in order
  * to access the `MailConfig` currently in use.
  */
trait Connection {

  def config: MailConfig

}
