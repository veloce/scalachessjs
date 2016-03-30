package scalachessjs

import scala.scalajs.js.JSApp
import scala.scalajs.js
import org.scalajs.dom
import js.Dynamic.{ newInstance => jsnew, literal => jsobj }
import js.JSConverters._

import chess.{ Board, Game, Color, Pos, PromotableRole }
import chess.variant.Variant

object Main extends JSApp {
  def main(): Unit = {

    val self = js.Dynamic.global

    self.addEventListener("message", { (e: dom.MessageEvent) =>
      val data = e.data.asInstanceOf[Message]
      val payload = data.payload.asInstanceOf[js.Dynamic]
      data.topic match {
        case "info" => {
          self.postMessage(Message(
            topic = "info",
            payload = "OK"
          ))
        }
        case "dests" => {
          val key = payload.variant.asInstanceOf[js.UndefOr[String]]
          key.toOption.flatMap(Variant(_)).fold(sendError(s"variant $key unknown")) { variant =>
            getDests(variant, payload.fen.asInstanceOf[String])
          }
        }
      }
    })

    def getDests(variant:Variant, fen: String): Unit = {
      val game = Game(Some(variant), Some(fen))
      self.postMessage(Message(
        topic = "dests",
        payload = jsobj(
          "dests" -> possibleDests(game)
        )
      ))
    }

    def sendError(error: String): Unit =
      self.postMessage(Message(
        topic = "error",
        payload = error
      ))
  }

  // private def move(game: Game, orig: Pos, dest: Pos, promotion: Option[PromotableRole]): js.Object = {
  //   game(orig, dest, promotion) map {
  //     case (game, move) =>
  //       val movable = !game.situation.end
  //       val fen = chess.format.Forsyth >> game
  //   }
  // }

  private def possibleDests(game: Game): js.Dictionary[js.Array[String]] = {
    game.situation.destinations.map {
      case (pos, dests) => (pos.toString -> dests.map(_.toString).toJSArray)
    }.toJSDictionary
  }

}

@js.native
trait Message extends js.Object {
  val topic: String
  val payload: js.Any
}

object Message {
  def apply(topic: String, payload: js.Any): Message =
    js.Dynamic.literal(topic = topic, payload = payload).asInstanceOf[Message]
}
