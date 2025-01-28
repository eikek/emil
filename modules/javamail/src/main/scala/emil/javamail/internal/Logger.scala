package emil.javamail.internal

import cats.effect.Sync

trait Logger {

  def trace(msg: => String): Unit
  def traceF[F[_]: Sync](msg: => String): F[Unit]

  def debug(msg: => String): Unit
  def debugF[F[_]: Sync](msg: => String): F[Unit]

  def info(msg: => String): Unit
  def infoF[F[_]: Sync](msg: => String): F[Unit]

  def warn(msg: => String): Unit
  def warnF[F[_]: Sync](msg: => String): F[Unit]

  def error(ex: Throwable)(msg: => String): Unit
  def errorF[F[_]: Sync](ex: Throwable)(msg: => String): F[Unit]

  def error(msg: => String): Unit
  def errorF[F[_]: Sync](msg: => String): F[Unit]

}

object Logger {

  def apply(clazz: Class[?]): Logger =
    apply(org.log4s.getLogger(clazz))

  def apply(logger: => org.log4s.Logger): Logger =
    new Logger {
      def trace(msg: => String): Unit =
        logger.trace(msg)

      def traceF[F[_]: Sync](msg: => String): F[Unit] =
        Sync[F].delay(trace(msg))

      def debug(msg: => String): Unit =
        logger.debug(msg)

      def debugF[F[_]: Sync](msg: => String): F[Unit] =
        Sync[F].delay(debug(msg))

      def info(msg: => String): Unit =
        logger.info(msg)

      def infoF[F[_]: Sync](msg: => String): F[Unit] =
        Sync[F].delay(info(msg))

      def warn(msg: => String): Unit =
        logger.warn(msg)

      def warnF[F[_]: Sync](msg: => String): F[Unit] =
        Sync[F].delay(logger.warn(msg))

      def error(ex: Throwable)(msg: => String): Unit =
        logger.error(ex)(msg)

      def errorF[F[_]: Sync](ex: Throwable)(msg: => String): F[Unit] =
        Sync[F].delay(error(ex)(msg))

      def error(msg: => String): Unit =
        logger.error(msg)

      def errorF[F[_]: Sync](msg: => String): F[Unit] =
        Sync[F].delay(error(msg))
    }
}
