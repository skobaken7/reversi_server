package actors

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import java.util.Calendar
import models._

class RoomActor extends Actor {
  case class Player(actor: ActorRef, name: String)

  var waiting = Option.empty[Player]
   
  def receive = {
    case Join(name)  => waiting match {
      case Some(black) => {
        waiting = None
        val white = Player(sender, name)
        val board = context.actorOf(BoardActor.props(black.actor, white.actor))
        black.actor ! State(board, Color.Black, white.name)
        white.actor ! State(board, Color.White, black.name)
      }

      case None => {
        waiting = Option(Player(sender, name))
      }
    }
  }
}

object RoomActor {
  def props = Props[RoomActor]
}
