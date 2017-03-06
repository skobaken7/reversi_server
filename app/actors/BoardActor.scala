package actors

import akka.actor.Actor
import akka.actor.ActorRef
import play.libs.Akka
import akka.actor.Props
import java.util.Calendar
import play.api.libs.json.JsValue
import models._
import org.joda.time.DateTime

class BoardActor(black: ActorRef, white: ActorRef) extends Actor{
  import scala.concurrent.duration._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  var lastPutTime = 0L
  var board = Board()

  def receive = {
    case e @ PutPoint(x, y, color) => try {
      val now = DateTime.now.getMillis
      lastPutTime = now

      board = board.put((x, y), color) 

      black ! PutResult(e, board)
      white ! PutResult(e, board)

      if (board.isFinished) {
        val result = Result(board.getCount(Color.Black), board.getCount(Color.White))
        black ! result
        white ! result
      }

      context.system.scheduler.scheduleOnce(2.seconds, self, Timeout(now, !color))
    } catch {
      case e: Throwable => sender ! Error(e.toString)
    }

    case e @ Timeout(sentTime, timeoutColor) => {
      if (sentTime == lastPutTime) {
        black ! e
        white ! e
      }
    }

    case LookBoard => sender ! board
  }
}

object BoardActor {
  def props(black: ActorRef, white: ActorRef) = Props(new BoardActor(black, white))
}
