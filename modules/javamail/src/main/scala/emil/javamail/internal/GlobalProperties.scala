package emil.javamail.internal

import cats.effect._

/** JavaMail uses various system properties to configure internals.
  * Those properties are often read at class loading time and then
  * cached in a static variable. Thus, it must be set as a system
  * property to be surely existent before any class loading happens.
  *
  * But the defaults are not good enough and it is tedious to always
  * run the apps with a correctly configured command line. So this
  * class holds certain system properties related to java mail that
  * are set programatically at a point where no javamail classes
  * should have been loaded yet (when using emil only).
  *
  * This class collects some javamail related system properties. They
  * are applied early enough (wrt to emil library). By default the
  * `lenient' set is applied. Properties are only set, if the current
  * value is absent. Thus, any system properties applied before are
  * retained and not overwritten by this class. To skip this, specify
  * the system property `emil.javamail.globalproperties=empty'. In
  * this case nothing is applied.
  */
case class GlobalProperties(props: Map[String, String]) {

  def foreach(f: (String, String) => Unit): Unit =
    props.foreach {
      case (k, v) =>
        f(k, v)
    }

  def map[A](f: (String, String) => A): List[A] =
    props.toList.map(t => f(t._1, t._2))
}

object GlobalProperties {

  val lenient = GlobalProperties(
    Map(
      // Don't be too strict with MIME parameter values in header fields
      // as they are sometimes invalid due to the various mail clients
      // that produce non-RFC conform fields (e.g. filename which
      // contains space characters not enclosed in quotes)
      "mail.mime.parameters.strict" -> "false",
      // the name parameter of the content-type is not really used
      // anymore
      "mail.mime.setcontenttypefilename" -> "false",
      // important: this breaks some limits defined in the rfc,
      // but otherwise it breaks too many mail clients
      "mail.mime.splitlongparameters" -> "false",
      // Interestingly, without these properties certain attachments
      // that are transfer-encoded using uuencode/uudecode fail. With
      // these properties they are correctly read.
      "mail.mime.uudecode.ignoreerrors"          -> "true",
      "mail.mime.uudecode.ignoremissingbeginend" -> "true",
      // Encode and decode filenames
      "mail.mime.encodefilename" -> "true",
      "mail.mime.decodefilename" -> "true"
    )
  )

  val empty = GlobalProperties(Map.empty)

  private val chooseSetName = "emil.javamail.globalproperties"

  private[javamail] def applySystemProperties[F[_]: Sync]: F[List[String]] =
    Sync[F].delay {
      val props = Option(System.getProperty(chooseSetName))
        .map(findSet)
        .getOrElse(lenient)
      setAll(props)
    }

  private[javamail] def findSet(name: String): GlobalProperties =
    name.toLowerCase match {
      case "empty"   => empty
      case "lenient" => lenient
      case _         => lenient
    }

  private[javamail] def setAll(props: GlobalProperties): List[String] =
    props.map(set).flatten

  private[javamail] def set(name: String, value: String): Option[String] =
    Option(System.getProperty(name)) match {
      case Some(_) =>
        None
      case None =>
        System.setProperty(name, value)
        Some(name)
    }
}
