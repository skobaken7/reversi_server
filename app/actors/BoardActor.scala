package actors

import akka.actor.Actor
import akka.actor.ActorRef
import play.libs.Akka
import akka.actor.Props
import akka.util._
import java.util.Calendar
import play.api.libs.json.JsValue
import models._

class BoardActor(black: ActorRef, white: ActorRef) extends Actor{
  var board = Board()

  def receive = {
    case e @ PutPoint(x, y, color) => try {
      board = board.put((x, y), color) 

      black ! PutResult(e, board)
      white ! PutResult(e, board)

      if (board.isFinished) {
        val result = Result(board.getCount(Color.Black), board.getCount(Color.White))
        black ! result
        white ! result
      }
    } catch {
      case e: Throwable => sender ! Error(e.toString)
    }

    case LookBoard => sender ! board
  }
}

object BoardActor {
  def props(black: ActorRef, white: ActorRef) = Props(new BoardActor(black, white))
}
