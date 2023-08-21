import fs2.io.net.Socket

case class ConnectedClient[F[_]](id: String, socket: Socket[F], username: Option[String] = None) {

  def withUsername(name: String) = this.copy(username = Some(name))

}
