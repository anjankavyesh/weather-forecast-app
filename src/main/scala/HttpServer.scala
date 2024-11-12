import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.model.HttpMethods.GET
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol._
import spray.json.{RootJsonFormat, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object HttpServer extends App {

  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "my-system")
  implicit val ec: ExecutionContext = system.executionContext

  final case class Coordination(latitude: Double, longitude: Double)

  implicit val coordinationFormat: RootJsonFormat[Coordination] = jsonFormat2(Coordination.apply)

  private final case class Result(shortForecast: String, temperature: String)

  private def categorizeTemperature(temperature: Int): String = {
    fahrenheitToCelsius(temperature) match {
      case t if t < 0 => "Very Cold"
      case t if t >= 0 && t < 10 => "Cold"
      case t if t >= 10 && t < 15 => "Cool"
      case t if t >= 15 && t < 25 => "Moderate"
      case t if t >= 25 && t < 30 => "Warm"
      case t if t >= 30 && t < 35 => "Hot"
      case t if t >= 35 => "Very Hot"
    }
  }

  private def fahrenheitToCelsius(fahrenheit: Double): Double =
    (fahrenheit - 32) * 5.0 / 9.0

  private def getGridPoints(forecastURL: String): Future[String] = {
    val request = HttpRequest(GET, uri = forecastURL)
    Http().singleRequest(request).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[String].map { response =>
          val geoJson = response.parseJson.asJsObject
          val maybeResult = for {
            properties <- geoJson.fields.get("properties")
            periods <- properties.asJsObject.fields.get("periods")
            period <- periods.convertTo[JsArray].elements.headOption
            shortForecast <- period.asJsObject.fields.get("shortForecast")
            temperature <- period.asJsObject.fields.get("temperature")
          } yield {
            val forecast = shortForecast.convertTo[String]
            val temp = temperature.convertTo[Int]

            Result(forecast, categorizeTemperature(temp))
          }

          maybeResult match {
            case Some(result) =>
              s"Short forecast: ${result.shortForecast}\nTemperature category: ${result.temperature}"
            case None =>
              throw new Exception("No period data found in forecast")
          }
        }
      case HttpResponse(code, _, _, _) =>
        throw new Exception(s"Failed to fetch forecast details: HTTP $code")
    }
  }

  private def getInitialRequestDetails(coordination: Coordination): Future[String] = {
    val url = s"https://api.weather.gov/points/${coordination.latitude},${coordination.longitude}"
    val request = HttpRequest(GET, uri = url)

    Http().singleRequest(request).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[String].map { response =>
          val geoJson = response.parseJson.asJsObject
          val maybeForecastURL = for {
            properties <- geoJson.fields.get("properties")
            forecast <- properties.asJsObject.fields.get("forecast")
          } yield forecast.convertTo[String]
          maybeForecastURL match {
            case Some(forecastURL) =>
              forecastURL
            case None =>
              throw new Exception("No forecast URL found")
          }
        }
      case HttpResponse(_, _, _, _) =>
        throw new Exception(s"Failed to fetch forecast details")
    }
  }

  private def getForecast(coordination: Coordination): Future[String] = {
    for {
      forecastURL <- getInitialRequestDetails(coordination)
      inner <- getGridPoints(forecastURL)
    } yield inner
  }.recover {
    case ex: Exception =>
      ex.getMessage
  }

  private val route: Route =
    pathSingleSlash {
      post {
        entity(as[Coordination]) { coord =>
          onSuccess(getForecast(coord)) { forecastDetails =>
            complete(forecastDetails)
          }
        }
      }
    }

  private val bindingFuture: Future[ServerBinding] =
    Http().newServerAt("localhost", 8080).bind(route)

  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
