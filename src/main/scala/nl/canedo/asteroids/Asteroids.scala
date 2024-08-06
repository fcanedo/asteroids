package nl.canedo.asteroids

import cats.effect.Concurrent
import cats.implicits.{catsSyntaxMonadError, toFunctorOps}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto
import io.circe.{Decoder, Encoder}
import nl.canedo.asteroids.Asteroids.Asteroid
import org.http4s.Method.GET
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{EntityDecoder, EntityEncoder, Header}
import org.typelevel.ci.CIStringSyntax

trait Asteroids[F[_]] {
  def asteroids(startDate: Option[String], endDate: Option[String]): F[List[Asteroid]]
  def asteroid(id: String): F[Asteroid]
}

object Asteroids {
  def apply[F[_]](implicit ev: Asteroids[F]): Asteroids[F] = ev

  implicit val customCirceConfiguration: Configuration = Configuration.default.withSnakeCaseMemberNames


  final case class AsteroidData(elementCount: Int, nearEarthObjects: Map[String, List[Asteroid]])

  object AsteroidData {
    implicit val asteroidDataDecoder: Decoder[AsteroidData] = deriveConfiguredDecoder[AsteroidData]
    implicit def asteroidDataEntityDecoder[F[_] : Concurrent]: EntityDecoder[F, AsteroidData] = jsonOf
    implicit val asteroidDataEncoder: Encoder[AsteroidData] = deriveConfiguredEncoder[AsteroidData]
    implicit def asteroidDataEntityEncoder[F[_]]: EntityEncoder[F, AsteroidData] = jsonEncoderOf
  }

  final case class Asteroid(id: String, name: String)

  object Asteroid {
    implicit val asteroidDecoder: Decoder[Asteroid] = semiauto.deriveDecoder[Asteroid]
    implicit def asteroidEntityDecoder[F[_] : Concurrent]: EntityDecoder[F, Asteroid] = jsonOf
    implicit val asteroidEncoder: Encoder[Asteroid] = semiauto.deriveEncoder[Asteroid]
    implicit def asteroidEntityEncoder[F[_]]: EntityEncoder[F, Asteroid] = jsonEncoderOf

    implicit def asteroidsListEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, List[Asteroid]] = jsonOf
    implicit def asteroidListEntityEncoder[F[_]]: EntityEncoder[F, List[Asteroid]] = jsonEncoderOf
  }

  final case class AsteroidError(e: Throwable) extends RuntimeException

  def impl[F[_] : Concurrent](C: Client[F]): Asteroids[F] = new Asteroids[F] {
    val dsl = new Http4sClientDsl[F] {}

    import dsl._

    def asteroids(startDate: Option[String], endDate: Option[String]): F[List[Asteroid]] = {
      val queryParams: Map[String, String] = Map(
        "start_date" -> startDate,
        "end_date" -> endDate,
        "apiKey" -> "DEMO_KEY"
      ).collect { case (key, Some(value: String)) => key -> value }

      C.expect[AsteroidData](
        GET((uri"https://api.nasa.gov" / "neo" / "rest" / "v1" / "feed")
          .withQueryParams(queryParams)
        ).withHeaders(Header.Raw(ci"Accept", "application/json")))
        .map { asteroidData =>
          asteroidData.nearEarthObjects.flatMap(_._2).toList.sortBy(_.name)
        }
        .adaptError { case t => AsteroidError(t) }
    }

    def asteroid(id: String): F[Asteroid] = {
      C.expect[Asteroid](
        GET((uri"https://api.nasa.gov" / "neo" / "rest" / "v1" / "neo" / id)
          .withQueryParams(Map("api_key" -> "DEMO_KEY"))
        ).withHeaders(Header.Raw(ci"Accept", "application/json")))
    }
  }
}
