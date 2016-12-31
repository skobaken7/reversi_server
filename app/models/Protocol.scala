package models

import play.api.libs.json._
import akka.actor.ActorRef

case class State(board: ActorRef, color: Color, oppositeName: String)
case class Join(name: String)
case class Matching(message: String)
case class Start(oppositeName: String, color: Color, board: Board)
case class PutPoint(x: Int, y: Int, color: Color)
case class PutResult(put: PutPoint, board: Board)
case class Result(black: Int, white: Int)
case class Error(exception: String)
case object LookBoard

object Implicits {
  import Color._

  implicit val cellsWrites = new Writes[Array[Array[Option[Color]]]] {
    def writes(cells: Array[Array[Option[Color]]]) = {
      JsArray(cells.map{ row => 
        JsArray(row.map{
          case Some(Black) => JsString("b")
          case Some(White) => JsString("w")
          case _ => JsString("x")
        }.toSeq)
      }.toSeq)
    }
  }

  implicit val boardWrites = Json.writes[Board]
  implicit val matchingFormat = Json.format[Matching]
  implicit val startWrites = Json.writes[Start]
  implicit val errorFormat = Json.format[Error]
  implicit val putColorFormat = Json.format[PutPoint]
  implicit val putResultWrites = Json.writes[PutResult]
  implicit val resultWrites = Json.writes[Result]
}
