package emil

import java.nio.charset.{CharacterCodingException, Charset, StandardCharsets}

import scodec.bits.ByteVector
import cats.Eval

private[emil] object PreferredCharsets extends Charsets {
  type DecodeResult = Either[CharacterCodingException, String]

  val iso = filterContains("iso")
  val utf = filterContains("utf")
  val win = filterContains("win")

  val get: List[Charset] =
    StandardCharsets.UTF_8 ::
      StandardCharsets.ISO_8859_1 ::
      iso.sorted ::: utf.sorted ::: win.sorted

  def decode(bv: ByteVector): DecodeResult = {
    def decode0(cs: Charset): Eval[DecodeResult] =
      Eval.later(bv.decodeString(cs))

    get
      .map(decode0)
      .find(_.value.isRight)
      .map(_.value)
      .getOrElse(Left(new CharacterCodingException))
  }
}
