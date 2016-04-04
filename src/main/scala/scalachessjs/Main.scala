package scalachessjs

import scala.scalajs.js.JSApp
import scala.scalajs.js
import org.scalajs.dom
import js.Dynamic.{ global => g, newInstance => jsnew, literal => jsobj }
import js.JSConverters._
import js.annotation._

import chess.{ Valid, Success, Failure, Board, Game, Color, Pos, Role, PromotableRole, Replay }
import chess.variant.Variant

object Main extends JSApp {
  def main(): Unit = {

    val self = js.Dynamic.global

    self.addEventListener("message", { e: dom.MessageEvent =>
      val data = e.data.asInstanceOf[Message]
      val payload = data.payload.asInstanceOf[js.Dynamic]
      val fen = payload.fen.asInstanceOf[js.UndefOr[String]].toOption
      val variantKey = payload.variant.asInstanceOf[js.UndefOr[String]].toOption
      val variant = variantKey.flatMap(Variant(_))

      data.topic match {

        case "init" => {
          init(variant, fen)
        }
        case "dests" => {
          fen.fold {
            sendError("fen field is required for dests topic")
          } { fen =>
            getDests(variant, fen)
          }
        }
        case "fenMove" => {
          val promotion = payload.promotion.asInstanceOf[js.UndefOr[String]].toOption
          val origS = payload.orig.asInstanceOf[String]
          val destS = payload.dest.asInstanceOf[String]
          val path = payload.path.asInstanceOf[js.UndefOr[String]].toOption
          (for {
            orig <- Pos.posAt(origS)
            dest <- Pos.posAt(destS)
            fen <- fen
          } yield (orig, dest, fen)) match {
            case Some((orig, dest, fen)) =>
              fenMove(variant, fen, orig, dest, Role.promotable(promotion), path)
            case None =>
              sendError(s"step topic params: $origS, $destS, $fen are not valid")
          }
        }
        case "pgnMove" => {
          val promotion = payload.promotion.asInstanceOf[js.UndefOr[String]].toOption
          val origS = payload.orig.asInstanceOf[String]
          val destS = payload.dest.asInstanceOf[String]
          val pgnMoves = payload.pgnMoves.asInstanceOf[js.Array[String]].toList
          val initialFen = payload.initialFen.asInstanceOf[js.UndefOr[String]].toOption
          val path = payload.path.asInstanceOf[js.UndefOr[String]].toOption

          (for {
            orig <- Pos.posAt(origS)
            dest <- Pos.posAt(destS)
            v <- variant
          } yield (orig, dest, v)) match {
            case Some((orig, dest, v)) =>
              pgnMove(v, initialFen, pgnMoves, orig, dest, Role.promotable(promotion))
            case None =>
              sendError(s"step topic params: $origS, $destS, $variant are not valid")
          }
        }
      }
    })

    def init(variant: Option[Variant], fen: Option[String]): Unit = {
      val game = Game(variant, fen)
      self.postMessage(Message(
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

    def getDests(variant: Option[Variant], fen: String): Unit = {
      val game = Game(variant, Some(fen))
      self.postMessage(Message(
        topic = "dests",
        payload = jsobj(
          "dests" -> possibleDests(game)
        )
      ))
    }

    def fenMove(variant: Option[Variant], fen: String, orig: Pos, dest: Pos, promotion: Option[PromotableRole], path: Option[String]): Unit = {
      val game = Game(variant, Some(fen))
      move(game, orig, dest, promotion) match {
        case Success(newSit) => {
          self.postMessage(Message(
            topic = "fenMove",
            payload = jsobj(
              "situation" -> newSit,
              "path" -> path.orUndefined
            )
          ))
        }
        case Failure(errors) => sendError(errors.head)
      }
    }

    def pgnMove(variant: Variant, initFen: Option[String], pgnMoves: List[String], orig: Pos, dest: Pos, promotion: Option[PromotableRole]): Unit = {
      val initialFen = initFen getOrElse chess.format.Forsyth.initial
      if (pgnMoves.isEmpty) {
        val game = Game(Some(variant), initFen)
        move(game, orig, dest, promotion) match {
          case Success(newSit) => {
            self.postMessage(Message(
              topic = "pgnMove",
              payload = jsobj(
                "initialFen" -> initialFen,
                "situation" -> newSit
              )
            ))
          }
          case Failure(errors) => sendError(errors.head)
        }
      } else {
        val vr = Replay(pgnMoves, initFen, variant)
        vr.flatMap(replay => move(replay.state, orig, dest, promotion)) match {
          case Success(newSit) => {
            self.postMessage(Message(
              topic = "pgnMove",
              payload = jsobj(
                "initialFen" -> initialFen,
                "situation" -> newSit
              )
            ))
          }
          case Failure(errors) => sendError(errors.head)
        }
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
        val movable = !newGame.situation.end
        gameToSituationInfo(newGame, promotion)
    }
  }

  private def gameToSituationInfo(game: Game, promotionRole: Option[PromotableRole] = None): js.Object = {
    val movable = !game.situation.end
    val emptyDests: js.Dictionary[js.Array[String]] = js.Dictionary()
    new SituationInfo {
      val variant = game.board.variant.key
      val fen = chess.format.Forsyth >> game
      val player = game.player.name
      val dests = if (movable) possibleDests(game) else emptyDests
      val playable = game.situation.playable(true)
      val status = game.situation.status.map { s =>
        jsobj(
          "id" -> s.id,
          "name" -> s.name
        )
      }.orUndefined
      val winner = game.situation.winner.map(_.name).orUndefined
      val check = game.situation.check
      val pgnMoves = game.pgnMoves.toJSArray
      val lastMove = game.board.history.lastMove.map { lm =>
        jsobj(
          "from" -> lm.origDest._1.toString,
          "to" -> lm.origDest._2.toString,
          "uci" -> lm.uci
        )
      }.orUndefined
      val promotion = promotionRole.map(_.forsyth).map(_.toString).orUndefined
      val ply = game.turns
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
  val playable: Boolean
  val status: js.UndefOr[js.Object]
  val winner: js.UndefOr[String]
  val check: Boolean
  val pgnMoves: js.Array[String]
  val lastMove: js.UndefOr[js.Object]
  val promotion: js.UndefOr[String]
  val ply: Int
}
