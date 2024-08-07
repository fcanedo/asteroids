package nl.canedo.asteroids

import cats.effect.{Async, Concurrent}
import cats.syntax.all._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import nl.canedo.asteroids.db.{Fav, Favs}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend

trait Favourites[F[_]] {
  def favourites: F[List[Fav]]
  def newFavourite(asteroidId: String): F[Unit]
}

object Favourites {

  object Favourite {
    implicit val favouriteDecoder: Decoder[Fav] = deriveDecoder[Fav]
    implicit def favouriteEntityDecoder[F[_] :Concurrent]: EntityDecoder[F, Fav] = jsonOf

    implicit val favouriteEncoder: Encoder[Fav] = deriveEncoder[Fav]
    implicit def favouriteEntityEncoder[F[_]]: EntityEncoder[F, Fav] = jsonEncoderOf

    implicit def favouritesListEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, List[Fav]] = jsonOf
    implicit def favouritesListEntityEncoder[F[_]]: EntityEncoder[F, List[Fav]] = jsonEncoderOf
  }

  def impl[F[_] :Async](db: backend.JdbcDatabaseDef): Favourites[F] = new Favourites[F] {
    val a = Async[F]

    def favourites: F[List[Fav]] = {
      a.fromFuture(a.pure(db.run(Favs.favourites.to[List].result)))
    }

    override def newFavourite(asteroidId: String): F[Unit] = {
      a.fromFuture(a.pure(db.run(Favs.favourites += Fav(asteroidId)))).map(_ => ())
    }
  }
}
