package controllers

import javax.inject.{Inject, Named}
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.Play.current
import actors._
import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.streams._
import akka.stream.Materializer

class Battle @Inject()(implicit system: ActorSystem, materializer: Materializer) extends Controller {
  val room = system.actorOf(RoomActor.props)

  def join(name: String) = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => UserActor.props(name, out, room))
  }
}
