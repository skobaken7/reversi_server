package actors

import models._
import models.Implicits._

import akka.actor.{Actor, ActorRef, Props, PoisonPill}
import akka.pattern.ask

import play.api.Logger
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration._


class UserActor(name: String, out: ActorRef, room: ActorRef) extends Actor {
  var state = Option.empty[State]
  implicit val timeout = akka.util.Timeout(1.minutes)

  override def preStart = {
    room ! Join(name)
    output("waiting", JsNull)
  }
  
  def receive = {
    case state: State if(this.state.isEmpty) => {
      this.state = Option(state)
      val future = state.board ? LookBoard
      future foreach { case board: Board =>
        output("start", Start(state.oppositeName, state.color, board))
      }
    }

    case e: Error => output("error", e)
    case result: PutResult => output("put", result)
    case result: Result => {
      output("result", result)
      self ! PoisonPill
    }

    case js: JsObject => {
      val action = (js \ "action").validate[String].asOpt.getOrElse("no action")
      input(action, js)
    }

    case _ => {
      output("error", Error(InvalidInputException.toString))
    }
  }

  def input(action: String, message: JsObject) = action match {
    case "put" if state.isDefined => {
      val Some(State(board, color, _)) = state
      val messageWithColor = message + ("color" -> JsString(color.toString))

      messageWithColor.validate[PutPoint].fold({ _ =>
        output("error", Error(InvalidInputException.toString))
      }, { param =>
        board ! param
      })
    }

    case e => output("error", Error(UnknownAction(e).toString))
  }

  def output[A](action: String, body: A)(implicit writes: Writes[A]) = {
    val whole = JsObject(Seq(
      "action" -> JsString(action),
      "body" -> Json.toJson(body)
    ))

    out ! whole
  }
}

object UserActor {
  def props(name: String, out: ActorRef, room: ActorRef) = Props(new UserActor(name, out, room))
}
