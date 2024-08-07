package nl.canedo.asteroids

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import Favourites.Favourite._

object FavouritesRoutes {
  def routes[F[_]: Sync](FV: Favourites[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "favourites" =>
        for {
          favourites <- FV.favourites
          resp <- Ok(favourites)
        } yield resp

      case PUT -> Root / "favourites" / asteroidId =>
        for {
          _ <- FV.newFavourite(asteroidId)
          resp <- Created()
        } yield resp
    }
  }
}
