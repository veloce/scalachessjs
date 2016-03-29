package scalachessjs

import scala.scalajs.js.JSApp
import chess.{ Valid, Success, Failure, Board, Game, Color, Pos }
import chess.variant.Variant
import chess.format.Forsyth

object Main extends JSApp {
  def main(): Unit = {

    def fenToGame(positionString: String, variant: Variant): Valid[Game] = {
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

    def possibleDests(game: Game, color: Color): Map[Pos, List[Pos]] = {
      val occ = game.board.occupation(color).toList
      occ.map(o => o -> game.board.destsFrom(o)).collect {
        case (p, Some(d)) if d.nonEmpty => (p, d)
      }.toMap
    }

    val game = fenToGame("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", chess.variant.Standard)
    game match {
      case Failure(f) => println(f)
      case Success(g) => {
        val pd = possibleDests(g, Color.white)
        println(s"game = $g")
        println(s"occupation = ${g.board.occupation}")
        println(s"dests = $pd")
      }
    }

  }
}
