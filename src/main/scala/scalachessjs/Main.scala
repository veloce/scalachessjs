package scalachessjs

import scala.scalajs.js.JSApp
import scala.scalajs.js
import org.scalajs.dom
import js.Dynamic.{ global => g, newInstance => jsnew, literal => jsobj }
import js.JSConverters._
import js.annotation._

import chess.{ Valid, Success, Failure, Board, Game, Color, Pos, Role, PromotableRole, Replay, Status, Move, MoveOrDrop }
import chess.variant.Variant
import chess.variant.Crazyhouse
import chess.format.UciDump
import chess.opening.FullOpeningDB

object Main extends JSApp {
  def main(): Unit = {

    val self = js.Dynamic.global

    self.addEventListener("message", { e: dom.MessageEvent =>

      try {
        val data = e.data.asInstanceOf[Message]
        val reqidOpt = data.reqid.asInstanceOf[js.UndefOr[String]].toOption
        val payload = data.payload.asInstanceOf[js.Dynamic]
        val fen = payload.fen.asInstanceOf[js.UndefOr[String]].toOption
        val variantKey = payload.variant.asInstanceOf[js.UndefOr[String]].toOption
        val variant = variantKey.flatMap(Variant(_))

        data.topic match {

          case "init" => {
            init(reqidOpt, variant, fen)
          }
          case "dests" => {
            val path = payload.path.asInstanceOf[js.UndefOr[String]].toOption
            fen.fold {
              sendError(reqidOpt, data.topic, "fen field is required for dests topic")
            } { fen =>
              getDests(reqidOpt, variant, fen, path)
            }
          }
          case "situation" => {
            val path = payload.path.asInstanceOf[js.UndefOr[String]].toOption
            fen.fold {
              sendError(reqidOpt, data.topic, "fen field is required for situation topic")
            } { fen =>
              val game = Game(variant, Some(fen))
              self.postMessage(Message(
                reqid = reqidOpt,
                topic = "situation",
                payload = jsobj(
                  "situation" -> gameToSituationInfo(game),
                  "path" -> path.orUndefined
                )
              ))
            }
          }
          case "threefoldTest" => {
            val pgnMoves = payload.pgnMoves.asInstanceOf[js.Array[String]].toList
            val initialFen = payload.initialFen.asInstanceOf[js.UndefOr[String]].toOption
            Replay(pgnMoves, initialFen, variant getOrElse Variant.default) match {
              case Success(replay) => {
                self.postMessage(Message(
                  reqid = reqidOpt,
                  topic = "threefoldTest",
                  payload = jsobj(
                    "threefoldRepetition" -> replay.state.board.history.threefoldRepetition,
                    "status" -> jsobj(
                      "id" -> Status.Draw.id,
                      "name" -> Status.Draw.name
                    )
                  )
                ))
              }
              case Failure(errors) => sendError(reqidOpt, data.topic, errors.head)
            }
          }
          case "move" => {
            val promotion = payload.promotion.asInstanceOf[js.UndefOr[String]].toOption
            val origS = payload.orig.asInstanceOf[String]
            val destS = payload.dest.asInstanceOf[String]
            val pgnMovesOpt = payload.pgnMoves.asInstanceOf[js.UndefOr[js.Array[String]]].toOption
            val uciMovesOpt = payload.uciMoves.asInstanceOf[js.UndefOr[js.Array[String]]].toOption
            val pgnMoves = pgnMovesOpt.map(_.toList).getOrElse(List.empty[String])
            val uciMoves = uciMovesOpt.map(_.toList).getOrElse(List.empty[String])
            val path = payload.path.asInstanceOf[js.UndefOr[String]].toOption
            (for {
              orig <- Pos.posAt(origS)
              dest <- Pos.posAt(destS)
              fen <- fen
            } yield (orig, dest, fen)) match {
              case Some((orig, dest, fen)) =>
                move(reqidOpt, variant, fen, pgnMoves, uciMoves, orig, dest, Role.promotable(promotion), path)
              case None =>
                sendError(reqidOpt, data.topic, s"step topic params: $origS, $destS, $fen are not valid")
            }
          }
          case "drop" => {
            val roleS = payload.role.asInstanceOf[String]
            val posS = payload.pos.asInstanceOf[String]
            val pgnMovesOpt = payload.pgnMoves.asInstanceOf[js.UndefOr[js.Array[String]]].toOption
            val uciMovesOpt = payload.uciMoves.asInstanceOf[js.UndefOr[js.Array[String]]].toOption
            val pgnMoves = pgnMovesOpt.map(_.toList).getOrElse(List.empty[String])
            val uciMoves = uciMovesOpt.map(_.toList).getOrElse(List.empty[String])
            val path = payload.path.asInstanceOf[js.UndefOr[String]].toOption
            (for {
              pos <- Pos.posAt(posS)
              role <- Role.allByName get roleS
              fen <- fen
            } yield (pos, role, fen)) match {
              case Some((pos, role, fen)) =>
                drop(reqidOpt, variant, fen, pgnMoves, uciMoves, role, pos, path)
              case None =>
                sendError(reqidOpt, data.topic, s"step topic params: $posS, $roleS, $fen are not valid")
            }
          }
          case "pgnRead" => {
            val pgn = payload.pgn.asInstanceOf[String]
            (for {
              replay <- chess.format.pgn.Reader.full(pgn)
              fen = chess.format.Forsyth >> replay.setup
              games <- replayGames(replay.chronoMoves, Some(fen), replay.setup.board.variant)
            } yield (replay, games)) match {
              case Success((replay, listOfGames)) => {
                self.postMessage(Message(
                  reqid = reqidOpt,
                  topic = "pgnRead",
                  payload = jsobj(
                    "variant" -> new VariantInfo {
                      val key = replay.setup.board.variant.key
                      val name = replay.setup.board.variant.name
                      val shortName = replay.setup.board.variant.shortName
                      val title = replay.setup.board.variant.title
                    },
                    "setup" -> gameToSituationInfo(replay.setup),
                    "replay" -> listOfGames.map(gameToSituationInfo(_)).toJSArray
                  )
                ))
              }
              case Failure(errors) => sendError(reqidOpt, data.topic, errors.head)
            }
          }
          case "pgnDump" => {
            val pgnMoves = payload.pgnMoves.asInstanceOf[js.Array[String]].toList
            val initialFen = payload.initialFen.asInstanceOf[js.UndefOr[String]].toOption
            val white = payload.white.asInstanceOf[js.UndefOr[String]].toOption
            val black = payload.black.asInstanceOf[js.UndefOr[String]].toOption
            val date = payload.date.asInstanceOf[js.UndefOr[String]].toOption
            Replay(pgnMoves, initialFen, variant getOrElse Variant.default) match {
              case Success(replay) => {
                val pgn = PgnDump(replay.state, initialFen, replay.setup.startedAtTurn + 1, white, black, date)
                self.postMessage(Message(
                  reqid = reqidOpt,
                  topic = "pgnDump",
                  payload = jsobj(
                    "pgn" -> pgn.toString
                  )
                ))
              }
              case Failure(errors) => sendError(reqidOpt, data.topic, errors.head)
            }
          }
          case "opening" => {
            val pgnMoves = payload.pgnMoves.asInstanceOf[js.Array[String]].toList
            FullOpeningDB.search(pgnMoves) match {
              case Some(opening) => self.postMessage(Message(
                topic = "opening",
                reqid = reqidOpt,
                payload = jsobj(
                  "opening" -> jsobj(
                    "eco" -> opening.opening.eco,
                    "name" -> opening.opening.name,
                    "fen" -> opening.opening.fen
                  ),
                  "ply" -> opening.ply
                )
              ))
              case None => self.postMessage(Message(
                topic = "opening",
                reqid = reqidOpt,
                payload = null
              ))
            }
          }
          case _ => {
            sendError(reqidOpt, data.topic, "Invalid command.")
          }
        }
      } catch {
        case ex: Exception => {
          val data = e.data.asInstanceOf[Message]
          val reqidOpt = data.reqid.asInstanceOf[js.UndefOr[String]].toOption
          sendError(reqidOpt, data.topic, "Exception caught in scalachessjs: " + ex)
        }
      }
    })

    def init(reqid: Option[String], variant: Option[Variant], fen: Option[String]): Unit = {
      val game = Game(variant, fen)
      self.postMessage(Message(
        reqid = reqid,
        topic = "init",
        payload = jsobj(
          "variant" -> new VariantInfo {
            val key = game.board.variant.key
            val name = game.board.variant.name
            val shortName = game.board.variant.shortName
            val title = game.board.variant.title
          },
          "setup" -> gameToSituationInfo(game)
        )
      ))
    }

    def getDests(reqid: Option[String], variant: Option[Variant], fen: String, path: Option[String]): Unit = {
      val game = Game(variant, Some(fen))
      val movable = !game.situation.end
      val dests = if (movable) possibleDests(game) else emptyDests
      self.postMessage(Message(
        reqid = reqid,
        topic = "dests",
        payload = jsobj(
          "dests" -> dests,
          "path" -> path.orUndefined
        )
      ))
    }

    def move(reqid: Option[String], variant: Option[Variant], fen: String, pgnMoves: List[String], uciMoves: List[String], orig: Pos, dest: Pos, promotion: Option[PromotableRole], path: Option[String]): Unit = {
      Game(variant, Some(fen))(orig, dest, promotion) match {
        case Success((newGame, move)) => {
          val ucilm = UciDump.move(newGame.board.variant)(Left(move))
          val mergedUciMoves = uciMoves :+ ucilm

          self.postMessage(Message(
            reqid = reqid,
            topic = "move",
            payload = jsobj(
              "situation" -> gameToSituationInfo(newGame.withPgnMoves(pgnMoves ++ newGame.pgnMoves), mergedUciMoves, promotion),
              "path" -> path.orUndefined
            )
          ))
        }
        case Failure(errors) => sendError(reqid, "move", errors.head)
      }
    }

    def drop(reqid: Option[String], variant: Option[Variant], fen: String, pgnMoves: List[String], uciMoves: List[String], role: Role, pos: Pos, path: Option[String]): Unit = {
      Game(variant, Some(fen)).drop(role, pos) match {
        case Success((newGame, drop)) => {
          val ucilm = UciDump.move(newGame.board.variant)(Right(drop))
          val mergedUciMoves = uciMoves :+ ucilm

          self.postMessage(Message(
            reqid = reqid,
            topic = "drop",
            payload = jsobj(
              "situation" -> gameToSituationInfo(newGame.withPgnMoves(pgnMoves ++ newGame.pgnMoves), mergedUciMoves),
              "path" -> path.orUndefined
            )
          ))
        }
        case Failure(errors) => sendError(reqid, "drop", errors.head)
      }
    }

    def sendError(reqid: Option[String], callerTopic: String, error: String): Unit =
      self.postMessage(Message(
        reqid = reqid,
        topic = "error",
        payload = jsobj(
          "callerTopic" -> callerTopic,
          "error" -> error
        )
      ))
  }

  private val emptyDests: js.Dictionary[js.Array[String]] = js.Dictionary()

  private def gameToSituationInfo(
    game: Game,
    mergedUciMoves: List[String] = List.empty[String],
    promotionRole: Option[PromotableRole] = None
  ): js.Object = {

    val movable = !game.situation.end

    new SituationInfo {
      val variant = game.board.variant.key
      val fen = chess.format.Forsyth >> game
      val player = game.player.name
      val dests = if (movable) possibleDests(game) else emptyDests
      val drops = possibleDrops(game)
      val end = game.situation.end
      val playable = game.situation.playable(true)
      val winner = game.situation.winner.map(_.name).orUndefined
      val check = game.situation.check
      val checkCount = jsobj(
        "white" -> game.board.history.checkCount.white,
        "black" -> game.board.history.checkCount.black
      )
      val pgnMoves = game.pgnMoves.toJSArray
      val uciMoves = mergedUciMoves.toJSArray
      val promotion = promotionRole.map(_.forsyth).map(_.toString).orUndefined
      val status = game.situation.status.map { s =>
        jsobj(
          "id" -> s.id,
          "name" -> s.name
          )
      }.orUndefined
      val crazyhouse = game.board.crazyData.map { d =>
        jsobj(
          "pockets" -> js.Array(
              d.pockets.white.roles.map(_.name).groupBy(identity).mapValues(_.size).toJSDictionary,
              d.pockets.black.roles.map(_.name).groupBy(identity).mapValues(_.size).toJSDictionary
            )
          )
      }.orUndefined
      val ply = game.turns
    }
  }

  private def possibleDests(game: Game): js.Dictionary[js.Array[String]] = {
    game.situation.destinations.map {
      case (pos, dests) => (pos.toString -> dests.map(_.toString).toJSArray)
    }.toJSDictionary
  }

  private def possibleDrops(game: Game): js.UndefOr[js.Array[String]] = {
    game.situation.drops.map { drops =>
      drops.map(_.toString).toJSArray
    }.orUndefined
  }

  private def replayGames(
    moves: List[MoveOrDrop],
    initialFen: Option[String],
    variant: chess.variant.Variant): Valid[List[Game]] = {
      val game = Game(Some(variant), initialFen)
      recursiveGames(game, moves) map { game :: _ }
  }

  private def recursiveGames(game: Game, moves: List[MoveOrDrop]): Valid[List[Game]] =
    moves match {
      case Nil => Success(Nil)
      case moveOrDrop :: rest => {
        val newGame = moveOrDrop.fold(game.apply, game.applyDrop)
        recursiveGames(newGame, rest) map { newGame :: _ }
      }
    }

}


@js.native
trait Message extends js.Object {
  val topic: String
  val payload: js.Any
  val reqid: js.UndefOr[String]
}

object Message {
  def apply(topic: String, payload: js.Any, reqid: Option[String]): Message =
    js.Dynamic.literal(topic = topic, payload = payload, reqid = reqid.orUndefined).asInstanceOf[Message]
}

@ScalaJSDefined
trait VariantInfo extends js.Object {
  val key: String
  val name: String
  val shortName: String
  val title: String
}

@ScalaJSDefined
trait SituationInfo extends js.Object {
  val variant: String
  val fen: String
  val player: String
  val dests: js.Dictionary[js.Array[String]]
  val drops: js.UndefOr[js.Array[String]]
  val end: Boolean
  val playable: Boolean
  val status: js.UndefOr[js.Object]
  val winner: js.UndefOr[String]
  val check: Boolean
  val checkCount: js.Object
  val pgnMoves: js.Array[String]
  val uciMoves: js.Array[String]
  val promotion: js.UndefOr[String]
  val crazyhouse: js.UndefOr[js.Object]
  val ply: Int
}
