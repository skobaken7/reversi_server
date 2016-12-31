package models

import enumeratum._

sealed trait Color extends EnumEntry {
  def reverse = this match {
    case Color.Black => Color.White
    case Color.White => Color.Black
  }

  def unary_! = reverse
}

object Color extends Enum[Color] with PlayJsonEnum[Color] {
  val values = findValues

  case object Black extends Color
  case object White extends Color
}
