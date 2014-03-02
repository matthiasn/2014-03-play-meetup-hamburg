package controllers

import play.api.mvc._
import shared.ChatMsg
import play.api.libs.json.JsValue
import org.scalajs.spickling._
import org.scalajs.spickling.playjson._
import play.api.libs.iteratee.{Enumeratee, Concurrent}
import play.api.libs.EventSource
import play.api.libs.concurrent.Execution.Implicits._
import Implicits._

object Application extends Controller {

  PicklerRegistry.register[ChatMsg]
  PicklerRegistry.register[Vector[ChatMsg]]

  /** Central hub for distributing chat messages */
  val (chatOut, chatChannel) = Concurrent.broadcast[ChatMsg]

  /** messages stored in an immutable vector (overwritten when new messages received) */
  var msgs = Vector[ChatMsg]()

  def index = Action { Ok(views.html.index("")) }

  /** Controller action for POSTing chat messages */
  def postMessage = Action(parse.json) {
    req => {
      val chatMsg: ChatMsg = PicklerRegistry.unpickle(req.body).asInstanceOf[ChatMsg]
      println(chatMsg)
      chatChannel.push(chatMsg)
      msgs = chatMsg +: msgs

      if (chatMsg.seq == 0) {
        val greeting: ChatMsg = ChatMsg("Hello " + chatMsg.name, "Server", chatMsg.timestamp, 1)
        chatChannel.push(greeting)
        msgs = greeting +: msgs
      }

      Ok(PicklerRegistry.pickle(chatMsg))
    }
  }

  /** Controller action for retrieving chat messages */
  def getMessages = Action { Ok(PicklerRegistry.pickle(msgs)) }

  /** Enumeratee for converting typed chat messages to JsValue */
  def chatMsg2JsValue: Enumeratee[ChatMsg, JsValue] = Enumeratee.map[ChatMsg] { msg => PicklerRegistry.pickle(msg) }

  /** Controller action serving activity based on room */
  def chatFeed = Action { req =>
    Ok.feed(chatOut
      &> Concurrent.buffer(50)
      &> chatMsg2JsValue
      &> EventSource()
    ).as("text/event-stream")
  }
}
