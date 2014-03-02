package example

import scala.scalajs.js
import js.Dynamic.{ global => g, literal => lit}
import org.scalajs.jquery.{jQuery => jQ}
import shared.ChatMsg

object ReactComponents {
  val nameField = jQ("#nameField")
  val textField = jQ("#textField")

  val ChatMsgComp = g.React.createClass(lit(
    render = {
      ths: js.Dynamic => {
        val msg = ths.props.chatMsg.asInstanceOf[ChatMsg]
        val cssClass = msg.name match {
          case "Server" => "msg server"
          case _ if (nameField.`val`() == msg.name)  => "msg"
          case _ => "msg others"
        }
        g.React.DOM.div(lit(className = cssClass), g.moment(msg.timestamp).format("HH:mm:ss"), " - ", msg.name, ": ", msg.text)
      }
    }: js.ThisFunction)
  )

  val ChatMsgsComp = g.React.createClass(lit(
    render = {
      ths: js.Dynamic => {
        val messages = ths.props.messages.asInstanceOf[Vector[ChatMsg]]
        val divs = messages.map { msg => ChatMsgComp(lit(chatMsg = msg.asInstanceOf[js.Any])) }.toArray
        g.React.DOM.div(null, divs)
      }
    }: js.ThisFunction)
  )

}
