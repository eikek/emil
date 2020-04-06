package emil.javamail.conv

sealed trait MessageIdEncode

object MessageIdEncode {
  case object Random        extends MessageIdEncode
  case object Given         extends MessageIdEncode
  case object GivenOrRandom extends MessageIdEncode
}
