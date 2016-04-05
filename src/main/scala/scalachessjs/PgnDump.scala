package scalachessjs

import chess.format.Forsyth
import chess.format.pgn.{ Pgn, Tag, Parser, ParsedPgn }
import chess.format.{ pgn => chessPgn }
import chess.Game

import scala.scalajs.js
import js.Dynamic.{ global => g, newInstance => jsnew }

object PgnDump {

  def apply(game: Game, initialFen: Option[String], startedAtTurn: Int): Pgn = {
    val ts = tags(game, initialFen)
    Pgn(ts, turns(game.pgnMoves, startedAtTurn))
  }

  private val customStartPosition: Set[chess.variant.Variant] =
    Set(chess.variant.Chess960, chess.variant.FromPosition, chess.variant.Horde, chess.variant.RacingKings)

  private def tags(
    game: Game,
    initialFen: Option[String]): List[Tag] = {
      val d = jsnew(g.Date)()
      List(
        Tag(_.Event, "Casual Game"),
        Tag(_.Date, d.toLocaleString()),
        Tag(_.White, "Anonymous"),
        Tag(_.Black, "Anonymous"),
        Tag(_.Result, result(game)),
        Tag("PlyCount", game.turns),
        Tag(_.FEN, initialFen getOrElse "?"),
        Tag(_.Variant, game.board.variant.name.capitalize),
        Tag(_.Termination, game.situation.status.fold("?")(s => s.name))
      )
  }

  private def turns(moves: List[String], from: Int): List[chessPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map {
      case (moves, index) => chessPgn.Turn(
        number = index + from,
        white = moves.headOption.filter(".." !=).map(s => chessPgn.Move(s)),
        black = moves.lift(1).map(s => chessPgn.Move(s)))
    } filterNot (_.isEmpty)

  private def result(game: Game) = game.situation.status.fold("*") { _ =>
    game.situation.winner.fold("1/2-1/2")(c => c.fold("1-0", "0-1"))
  }
}
