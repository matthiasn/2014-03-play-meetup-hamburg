package example

import scala.scalajs.js
import js.Dynamic.{ global => g, newInstance => jsnew, literal => lit}
import shared.ChatMsg
import org.scalajs.spickling._
import org.scalajs.spickling.jsany._
import org.scalajs.jquery.{jQuery => jQ}
import scala.scalajs.js.JSON
import Implicits._

trait MessageObject extends js.Object {
  val data: js.String = ???
}

object ScalaJSExample {
  var counter = 0

  /** messages stored in an immutable vector (overwritten when new messages received) */
  var msgs = Vector[ChatMsg]()

  /** Register case classes for pickling / de-serialization */
  PicklerRegistry.register[ChatMsg]
  PicklerRegistry.register[Vector[ChatMsg]]

  def now = jsnew(g.Date)()

  def addMessage(obj: MessageObject) = {
    val parsed: js.Any = JSON.parse(obj.data)
    val msg = PicklerRegistry.unpickle(parsed).asInstanceOf[ChatMsg]
    msgs = msg +: msgs
    tlComp.setProps(lit(messages = msgs.asInstanceOf[js.Any]))
  }

  def addPrev(data: js.Any): Unit = {
    PicklerRegistry.unpickle(data) match {
      case v: Vector[ChatMsg] => {
        msgs = v
        tlComp.setProps(lit(messages = msgs.asInstanceOf[js.Any]))
      }
    }
  }

  val nameField = jQ("#nameField")
  val textField = jQ("#textField")

  def submit(): Unit = {
    Ajax.post(PicklerRegistry.pickle(ChatMsg(textField.value().toString, nameField.value().toString, now.toString, counter)))
    counter = counter + 1
  }

  /** Set up server sent event stream and attach event listener */
  val stream = jsnew(g.EventSource)("/chatFeed")
  stream.addEventListener("message", addMessage _)

  Ajax.loadPrev()

  val tlComp = g.React.renderComponent(ReactComponents.ChatMsgsComp(lit(messages = msgs.asInstanceOf[js.Any])),
                                       g.document.getElementById("chat"))

}
