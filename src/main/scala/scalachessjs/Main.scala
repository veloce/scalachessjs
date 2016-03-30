package scalachessjs

import scala.scalajs.js.JSApp
import scala.scalajs.js
import org.scalajs.dom
import js.Dynamic.{ global => g, newInstance => jsnew, literal => obj }

import chess.{ Valid, Success, Failure, Board, Game, Color, Pos }
import chess.variant.Variant
import chess.format.Forsyth

@js.native
trait Message extends js.Object {
  val topic: String
  val payload: js.Object
}

object Main extends JSApp {
  def main(): Unit = {

    val self = js.Dynamic.global

    self.addEventListener("message", { (e: dom.MessageEvent) =>
      val data = e.data.asInstanceOf[Message]
      val payload = data.payload.asInstanceOf[js.Dynamic]
      data.topic match {
        case "dests" => getDests(payload.fen.asInstanceOf[String], chess.variant.Standard)
      }
    })

    def getDests(fen: String, variant: Variant): Unit = {
      val game = fenToGame(fen, variant)
      game.fold(e => sendError(e.head), g => {
        self.postMessage(obj(
          "topic" -> "dests",
          "payload" -> obj(
            "dests" -> possibleDests(g, g.player)
          )
        ))
      })
    }

    def sendError(error: String): Unit =
      self.postMessage(obj(
        "topic" -> "dests",
        "payload" -> obj(
          "error" -> error
        )
      ))
  }

  private def fenToGame(positionString: String, variant: Variant): Valid[Game] = {
    val situation = Forsyth << positionString
    (situation.map { sit =>
      sit.color -> sit.withVariant(variant).board
    } match {
      case Some(sit) => chess.success(sit)
      case None => chess.failure("Could not construct situation from FEN" )
    }).map {
      case (color, board) => Game(variant).copy(board = board) withPlayer color
    }
  }

  private def possibleDests(game: Game, color: Color): Map[Pos, List[Pos]] = {
    val occ = game.board.occupation(color).toList
    occ.map(o => o -> game.board.destsFrom(o)).collect {
      case (p, Some(d)) if d.nonEmpty => (p, d)
    }.toMap
  }

}
