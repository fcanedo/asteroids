package nl.canedo.asteroids

import cats.effect.{Async, Resource, Sync}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.io.net.Network
import nl.canedo.asteroids.db.Favs
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend

object AsteroidsServer {

  def getDB[F[_] :Async](implicit F: Sync[F]): Resource[F, backend.JdbcDatabaseDef] = {
    Resource.make {
      F.blocking(Database.forConfig("postgres"))
    }{ db =>
      F.blocking(db.close())
    }
  }

  def run[F[_] : Async : Network]: F[Nothing] = {
    for {
      client <- EmberClientBuilder.default[F].build
      db <- getDB
      _ = db.run(Favs.favourites.result)
      favourites = Favourites.impl[F](db)
      asteroidsAlg = Asteroids.impl[F](client)

      httpApp = (AsteroidsRoutes.routes(asteroidsAlg) <+>
        FavouritesRoutes.routes(favourites)).orNotFound

      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      _ <-
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever
}
