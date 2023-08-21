import Command._
import ConnectedClient._
import cats.Monad
import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import com.comcast.ip4s._
import fs2._
import fs2.io.net._

import scala.util.chaining._
import java.util.UUID
import cats.ApplicativeError
import cats.MonadError

class ChatServer[F[_]: Async: Network: Console](listeningPort: Int, clients: Clients[F]) {

  def create() =
    Network[F]
      .server(port = Port.fromInt(listeningPort))
      .flatMap(socket => Stream.eval(newClient(socket)))
      .flatTap(_ => Stream.eval(Console[F].println("Client joined")))
      .map { client =>
        client.socket.reads
          .through(text.utf8Decode)
          .map(Command.fromString)
          .flatMap(cmd => processCommand(cmd, client))
          .handleErrorWith(_ => Stream.eval(Async[F].delay(println("remove client"))))
      }

  private def newClient(
      socket: Socket[F]
  ): F[ConnectedClient[F]] = newId().map(ConnectedClient(_, socket))

  private def newId(): F[String] = Async[F].delay(UUID.randomUUID().toString())

  private def processCommand(
      maybeCommand: Option[Command],
      client: ConnectedClient[F]
  ): Stream[F, Unit] =
    maybeCommand match
      case None => Stream.eval(client.socket.write(Chunk.array("Invalid command\n".getBytes())))
      case Some(value) =>
        value match
          case Register(username) => Stream.eval(handleRegister(client.withUsername(username)))
          case SendMessage(msg)   => Stream.eval(handleSendMessage(Message(msg, client)))
          case _                  => Stream.eval(Async[F].delay(println("not implemented")))

  private def handleRegister(client: ConnectedClient[F]): F[Unit] = clients.add(client).flatMap {
    case false => client.socket.write(Chunk.array("Client already registered\n".getBytes()))
    case true  => Async[F].unit
  }

  private def handleSendMessage(
      message: Message[F]
  ): F[Unit] = clients.isRegistered(message.sender).flatMap {
    case true => clients.broadcast(message)
    case false =>
      message.sender.socket
        .write(Chunk.array("Cannot send message, you are not registered\n".getBytes()))
  }

}

object ChatServer {

  def make[F[_]: Async: Network: Console](port: Int) =
    Clients.make[F].map(clients => new ChatServer[F](port, clients))

}
