package nl.canedo.asteroids

import cats.effect.Async
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

object AsteroidsServer {

  def run[F[_] : Async : Network]: F[Nothing] = {
    for {
      client <- EmberClientBuilder.default[F].build
      asteroidsAlg = Asteroids.impl[F](client)

      httpApp = AsteroidsRoutes.routes(asteroidsAlg).orNotFound

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
