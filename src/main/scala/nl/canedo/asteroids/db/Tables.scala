package nl.canedo.asteroids.db

import slick.jdbc.PostgresProfile.api._

final case class Fav(asteroidId: String)

class Favs(tag: Tag) extends Table[Fav](tag, "favourites") {
  def asteroidId = column[String]("asteroid_id", O.PrimaryKey)

  def * = asteroidId <> (Fav.apply, { f: Fav => Some(f.asteroidId) })
}

object Favs {
  val favourites = TableQuery[Favs]
  val all: Query[Favs, Fav, Seq] = favourites.filter(_ => true)
}