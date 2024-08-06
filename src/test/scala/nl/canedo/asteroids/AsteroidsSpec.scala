package nl.canedo.asteroids

import cats.effect.{IO, Resource}
import munit.CatsEffectSuite
import nl.canedo.asteroids.Asteroids.{Asteroid, AsteroidData}
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Response, Status}

class AsteroidsSpec extends CatsEffectSuite {
  test("asteroids returns an empty list") {
    val obtained = retAsteroids(Response[IO](status = Status.Ok).withEntity(AsteroidData(0, Map.empty)))
    assertIO(obtained.flatMap(_.as[String]), "[]")
  }

  test("asteroids returns the asteroid") {
    val obtained = retAsteroids(Response[IO](status = Status.Ok).withEntity(AsteroidData(1, Map("2015-09-08" -> List(Asteroid("12345", "Sagittarius A*"))))))
    assertIO(obtained.flatMap(_.as[String]), """[{"id":"12345","name":"Sagittarius A*"}]""")
  }

  test("asteroids returns the asteroids") {
    val obtained = retAsteroids(Response[IO](status = Status.Ok).withEntity(AsteroidData(2, Map("2015-09-08" -> List(Asteroid("12345", "Sagittarius A*")), "2015-09-09" -> List(Asteroid("12346", "Apophis"))))))
    assertIO(obtained.flatMap(_.as[List[Asteroid]].map(_.map(_.id))), List("12346", "12345"))

  }
  test("returns the requested asteroid") {
    val obtained = retAsteroid(Response[IO](status = Status.Ok).withEntity(Asteroid("12345", "Sagittarius A*")))
    assertIO(obtained.flatMap(_.as[Asteroid]), Asteroid("12345", "Sagittarius A*"))
  }

  private[this] object MockHttpClient {
    def apply(response: Response[IO]): Client[IO] = Client[IO] { _ =>
      Resource.pure(response)
    }
  }

  private[this] def retAsteroid(response: Response[IO]): IO[Response[IO]] = {
    val getAsteroid = Request[IO](Method.GET, uri"asteroid/12345")
    val asteroids = Asteroids.impl[IO](MockHttpClient(response))
    AsteroidsRoutes.routes(asteroids).orNotFound(getAsteroid)
  }
  private[this] def retAsteroids(response: Response[IO]): IO[Response[IO]] = {
    val getAsteroids = Request[IO](Method.GET, uri"asteroids")
    val asteroids = Asteroids.impl[IO](MockHttpClient(response))
    AsteroidsRoutes.routes(asteroids).orNotFound(getAsteroids)
  }
}
