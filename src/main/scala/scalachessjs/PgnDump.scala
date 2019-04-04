package scalachessjs

import chess.format.pgn.{ Pgn, Tag, Tags }
import chess.format.{ pgn => chessPgn }
import chess.Game

import scala.scalajs.js
import js.Dynamic.{ global => g, newInstance => jsnew }

object PgnDump {

  def apply(
    game: Game,
    initialFen: Option[String],
    startedAtTurn: Int,
    white: Option[String] = None,
    black: Option[String] = None,
    date: Option[String] = None): Pgn = {
    val ts = tags(game, initialFen, white, black, date)
    Pgn(ts, turns(game.pgnMoves, startedAtTurn))
  }

  private def tags(
    game: Game,
    initialFen: Option[String],
    white: Option[String] = None,
    black: Option[String] = None,
    date: Option[String] = None): Tags = {
      val d = jsnew(g.Date)()
      Tags(List(
        Tag(_.Event, "Casual Game"),
        Tag(_.Site, "https://lichess.org"),
        Tag(_.Date, date getOrElse d.toLocaleString()),
        Tag(_.White, white getOrElse "Anonymous"),
        Tag(_.Black, black getOrElse "Anonymous"),
        Tag(_.Result, result(game)),
        Tag("PlyCount", game.turns),
        Tag(_.FEN, initialFen getOrElse "?"),
        Tag(_.Variant, game.board.variant.name.capitalize),
        Tag(_.Termination, game.situation.status.fold("?")(s => s.name))
      ))
  }

  private def turns(moves: Vector[String], from: Int): List[chessPgn.Turn] =
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
