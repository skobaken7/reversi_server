import play.api._
import play.api.mvc.Results._
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import scala.concurrent.Future
import play.api.Logger

object Global extends GlobalSettings {
  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error(ex.getMessage, ex)
    Future.successful(InternalServerError(Json.toJson(false)))
  }
}