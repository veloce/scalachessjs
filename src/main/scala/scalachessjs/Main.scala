package scalachessjs

import scala.scalajs.js.JSApp

object Main extends JSApp {
  def main(): Unit = {

    val board = chess.Board.init(chess.variant.Standard)

    println(s"board = $board")

  }
}
