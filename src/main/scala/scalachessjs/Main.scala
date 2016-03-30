package scalachessjs

import scala.scalajs.js.JSApp
import scala.scalajs.js
import org.scalajs.dom
import js.Dynamic.{ newInstance => jsnew, literal => jsobj }
import js.JSConverters._
import js.annotation._

import chess.{ Valid, Success, Failure, Board, Game, Color, Pos, Role, PromotableRole }
import chess.variant.Variant

object Main extends JSApp {
  def main(): Unit = {

    val self = js.Dynamic.global

    self.addEventListener("message", { e: dom.MessageEvent =>
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
          val fen = payload.fen.asInstanceOf[String]
          key.toOption.flatMap(Variant(_)).fold(sendError(s"variant $key unknown")) { variant =>
            getDests(variant, fen)
          }
        }
        case "move" => {
          val key = payload.variant.asInstanceOf[js.UndefOr[String]]
          val variant = key.toOption.flatMap(Variant(_)) getOrElse Variant.default
          val fen = payload.fen.asInstanceOf[String]
          val promotion = payload.promotion.asInstanceOf[js.UndefOr[String]]
          val origS = payload.orig.asInstanceOf[String]
          val destS = payload.dest.asInstanceOf[String]
          (for {
            orig <- Pos.posAt(origS)
            dest <- Pos.posAt(destS)
          } yield {
            getMove(variant, fen, orig, dest, Role.promotable(promotion.toOption))
          }) orElse {
            Some(sendError(s"move topic params: $origS, $destS are not valid"))
          }
        }
      }
    })

    def getDests(variant: Variant, fen: String): Unit = {
      val game = Game(Some(variant), Some(fen))
      self.postMessage(Message(
        topic = "dests",
        payload = jsobj(
          "dests" -> possibleDests(game)
        )
      ))
    }

    def getMove(variant: Variant, fen: String, orig: Pos, dest: Pos, promotion: Option[PromotableRole]): Unit = {
      val game = Game(Some(variant), Some(fen))
      move(game, orig, dest, promotion) match {
        case Success(move) => {
          self.postMessage(Message(
            topic = "move",
            payload = move
          ))
        }
        case Failure(errors) => sendError(errors.head)
      }
    }

    def sendError(error: String): Unit =
      self.postMessage(Message(
        topic = "error",
        payload = error
      ))
  }

  private def move(game: Game, orig: Pos, dest: Pos, promotion: Option[PromotableRole]): Valid[js.Object] = {
    game(orig, dest, promotion) map {
      case (newGame, move) =>
        new MovePayload {
          val fen = chess.format.Forsyth >> newGame
          val player = newGame.player.toString
          val movable = !newGame.situation.end
          val dests = (if (movable) Some(possibleDests(newGame)) else None).orUndefined
          val status = newGame.situation.status.map { s =>
            new js.Object {
              val id = s.id
              val name = s.name
            }
          }.orUndefined
          val check = newGame.situation.check
          val san = newGame.pgnMoves.last
          val lastMove = js.Array(move.orig.toString, move.dest.toString)
          val ply = newGame.turns
          val promotionLetter = promotion.map(_.forsyth).map(_.toString).orUndefined
        }
    }
  }

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

@ScalaJSDefined
trait MovePayload extends js.Object {
  val fen: String
  val player: String
  val movable: Boolean
  val dests: js.UndefOr[js.Dictionary[js.Array[String]]]
  val status: js.UndefOr[js.Object]
  val check: Boolean
  val lastMove: js.Array[String]
  val san: String
  val ply: Int
  val promotionLetter: js.UndefOr[String]
}
