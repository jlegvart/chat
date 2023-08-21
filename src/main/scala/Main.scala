import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all._
import fs2._
import fs2.io.net._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      server <- ChatServer.make[IO](5555)
      _      <- server.create().parJoinUnbounded.compile.drain
    } yield ExitCode.Success

}
