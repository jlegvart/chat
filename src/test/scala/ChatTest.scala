import org.scalatest._
import flatspec._
import org.scalatest.matchers.should.Matchers
import fs2.io.net.Network
import cats.effect.IO
import com.comcast.ip4s._
import fs2.Chunk
import cats.effect.std.Console
import cats.syntax.all._
import cats.effect.unsafe.implicits.global
import scala.concurrent.duration._
import java.nio.charset.Charset
import fs2.io.net.Socket
import cats.effect.kernel.Ref

class ChatTest extends AnyFlatSpec with Matchers {

  it should "create a server and connect few clients" in {
    val run = for {
      server <- ChatServer.make[IO](5555)
      _      <- server.create().parJoinUnbounded.compile.drain
    } yield ()

    val program = for {
      _         <- Console[IO].println("Starting server")
      server    <- run.start
      _         <- IO.sleep(1.seconds)
      state     <- Ref.of[IO, List[String]](List.empty[String])
      _         <- Console[IO].println("Creating sender client")
      sender    <- createClient(name = "sender")
      _         <- Console[IO].println("Creating receiver clients")
      receivers <- createClients(10)
      readerFib <- startReading(receivers, state).start
      _         <- IO.sleep(1.seconds)
      _         <- sender.write(Chunk.array("Hello from sender".getBytes))
      _         <- IO.sleep(1.seconds)
      _         <- IO.println("Completed")
      _         <- readerFib.cancel
      _         <- server.cancel
    } yield state

    val result = program.unsafeRunSync().get.unsafeRunSync()

    result shouldEqual (List.fill(10)("sender:Hello from sender"))
  }

  private def createClients(num: Int): IO[List[Socket[IO]]] =
    (1 to num).map(i => createClient(s"client-$i")).toList.sequence

  private def createClient(name: String): IO[Socket[IO]] =
    Network[IO].client(SocketAddress(host"localhost", port"5555")).allocated._1F.flatTap { socket =>
      socket.write(Chunk.array(s"register\\r\\n$name".getBytes))
    }

  private def startReading(sockets: List[Socket[IO]], ref: Ref[IO, List[String]]): IO[Unit] =
    sockets
      .map { socket =>
        socket
          .read(8192)
          .flatMap {
            case None => IO.unit
            case Some(read) =>
              val response = String(read.toArray, Charset.defaultCharset())
              Console[IO].println(response) >> ref.update(_ :+ response)
          }
      }
      .sequence
      .foreverM

}
