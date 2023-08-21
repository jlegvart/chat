import cats.Monad
import cats.syntax.all._
import cats.effect.kernel.Ref
import cats.ApplicativeError
import fs2.Chunk
import cats.effect.kernel.Sync

class Clients[F[_]: Monad](ref: Ref[F, Map[String, ConnectedClient[F]]]) {

  def add(client: ConnectedClient[F]): F[Boolean] = ref.modify { clientMap =>
    if (!clientMap.isDefinedAt(client.id)) {
      (clientMap + (client.id -> client), true)
    } else
      (clientMap, false)
  }

  def remove(clientId: String): F[Unit] = ref.update {
    _.removed(clientId)
  }

  def isRegistered(client: ConnectedClient[F]): F[Boolean] = ref.get.map(_.isDefinedAt(client.id))

  def broadcast(message: Message[F])(implicit ae: ApplicativeError[F, Throwable]): F[Unit] =
    for {
      clients <- ref.get
      sender = clients.get(message.sender.id).flatMap(_.username).getOrElse("None")
      write =
        clients.values
          .filterNot(_.id == message.sender.id)
          .map(send(sender, message))
          .toList
      _ <- write.sequence
    } yield ()

  def send(sender: String, message: Message[F])(
      client: ConnectedClient[F]
  )(implicit ae: ApplicativeError[F, Throwable]): F[Unit] =
    client.socket
      .write(Chunk.array((s"$sender:" + message.message).getBytes()))
      .handleErrorWith(_ => remove(client.id))

}

object Clients {

  def make[F[_]: Sync]: F[Clients[F]] = Ref.of(Map.empty[String, ConnectedClient[F]]).map(new Clients[F](_))

}
