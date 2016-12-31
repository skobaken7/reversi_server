package models

case class Board(cells: Array[Array[Option[Color]]], nextTurn: Option[Color]) {
  type Point = (Int, Int)

  private def arrounds = for(i <- -1 to 1; j <- -1 to 1 if (!(i == 0 && j == 0))) yield (i, j)

  private def getOppositeCellsUntilSameColor(from: Point, color: Color)(direction: Point): List[Point] = {
    val (dx, dy) = direction

    def loop(x: Int, y: Int, result: List[Point]): List[Point] = if (!isValidPoint(x, y)) {
      List.empty
    } else {
      cells(x)(y) match {
        case None => List.empty
        case Some(c) => if (c == color) {
          result.reverse
        } else {
          loop(x+dx, y+dy, (x,y)::result)
        }
      }
    }

    loop(from._1 + dx, from._2 + dy, List.empty)
  }

  def cell(p: Point) = cells(p._1)(p._2)

  private def isValidPut(p: Point, color: Color): Boolean = {
    if (!isValidPoint(p) || cell(p).isDefined) return false
    
    val reversed = arrounds flatMap getOppositeCellsUntilSameColor(p, color)
    reversed.size > 0
  }

  def candidates(color: Color): Seq[Point] = {
    val all = for(i <- 0 until Board.WIDTH; j <- 0 until Board.WIDTH) yield (i, j)
    all filter (p => isValidPut(p, color))
  }

  def put(p: Point, color: Color): Board = nextTurn map { nextColor =>
    if (color != nextColor) throw OppositeTurnException
    if (!isValidPoint(p))  throw InvalidPointException
    if (cell(p).isDefined) throw StoneAlreadyExistsException
    
    val reversed = arrounds flatMap getOppositeCellsUntilSameColor(p, color)
    if (reversed.size == 0) throw InvalidPutException

    val newCells = cells.map(_.clone)
    newCells(p._1)(p._2) = Option(color)
    for ((x, y) <- reversed) {
      newCells(x)(y) = Option(color)
    }

    val nextBoard = Board(newCells, None)

    if (nextBoard.candidates(!nextColor).size > 0) {
      Board(newCells, Some(!nextColor))
    } else if (nextBoard.candidates(nextColor).size > 0) {
      Board(newCells, Some(nextColor))
    } else {
      nextBoard
    }
  } getOrElse {
    throw InvalidPutException
  }

  private def isValidPoint(p: Point) = p._1 >= 0 && p._1 < Board.WIDTH && p._2 >= 0 && p._2 < Board.WIDTH

  def isFinished = nextTurn.isEmpty

  def getCount(color: Color) = cells.flatten.flatten.filter(_ == color).size
}

object Board {
  val WIDTH = 8

  def apply() = {
    val cells = Array.fill(WIDTH) {
      Array.fill(WIDTH) {
        Option.empty[Color]
      }
    }

    cells(3)(3) = Option(Color.White)
    cells(4)(4) = Option(Color.White)
    cells(3)(4) = Option(Color.Black)
    cells(4)(3) = Option(Color.Black)

    new Board(cells, Option(Color.Black))
  }
}
