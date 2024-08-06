package nl.canedo.asteroids

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object AsteroidsRoutes {

  private object startDateMatcher extends OptionalQueryParamDecoderMatcher[String]("startDate")
  private object endDateMatcher extends OptionalQueryParamDecoderMatcher[String]("endDate")

  def routes[F[_]: Sync](A: Asteroids[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "asteroid" / id =>
        for {
          asteroid <- A.asteroid(id)
          resp <- Ok(asteroid)
        } yield resp
      case GET -> Root / "asteroids" :? startDateMatcher(startDate) +& endDateMatcher(endDate) =>
        for {
          asteroids <- A.asteroids(startDate, endDate)
          resp <- Ok(asteroids)
        } yield resp
    }
  }
}