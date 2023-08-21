import cats.syntax.all._

trait Command

object Command {

  case class Register(username: String) extends Command
  case class SendMessage(msg: String)   extends Command

  def fromString(command: String): Option[Command] =
    if (command.isBlank()) None
    else {
      val split = command.trim().split("\\\\r\\\\n")

      if (split.size == 1)
        SendMessage(command).some
      else {
        split(0) match {
          case "register" => Register(split(1)).some
          case _          => ???
        }
      }
    }

}
