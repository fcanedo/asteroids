package nl.canedo.asteroids

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run = AsteroidsServer.run[IO]
}
