require 'websocket-client-simple'
require 'json'
require 'pp'

name = ARGV[0]

ws = WebSocket::Client::Simple.connect "ws://localhost:9000/battle?name=#{name}"

WIDTH = 8
BLACK = "○"
WHITE = "●"
EMPTY = "×"

def short_color(color)
  color[0,1].downcase
end

class Board
  def initialize(cells)
    @cells = cells
  end

  def candidates(color)
    idx = []
    for x in 0...WIDTH
      for y in 0...WIDTH
        idx << [x,y]
      end
    end

    idx.shuffle!

    res = []
    for p in idx
      res << p if is_valid_put(p[0], p[1], color)

      # break if !size.nil? && res.size >= size
    end

    res
  end

  def is_empty(x, y)
    @cells[x][y] == 'x'
  end

  def is_valid_put(x, y, color)
    return false if !is_valid_point(x, y) || !is_empty(x, y)

    res = []
    for i in -1..1
      for j in -1..1
        next if i == 0 && j == 0

        res << get_opposite_cells_until_same_color(x, y, color, i, j)
      end
    end

    res.flatten.size > 0
  end

  def is_valid_point(x, y)
    0 <= x && x < WIDTH && 0 <= y && y < WIDTH
  end

  def get_opposite_cells_until_same_color(cx, cy, color, dx, dy)
    x = cx
    y = cy

    res = []
    loop do
      x += dx
      y += dy

      return [] if !is_valid_point(x, y) || is_empty(x, y)
      
      if @cells[x][y] == color
        return res
      else
        res << [x,y]
      end
    end
    
    res
  end

  def print
    puts @cells.transpose.map{|row| row.join("")}.join("\n").gsub("w", WHITE).gsub("b", BLACK).gsub("x", EMPTY)
    puts ("-"*20)
  end

  def put(x, y, color)
    raise Exception.new("invalid point") unless is_valid_point(x, y)
    raise Exception.new("not empty") unless is_empty(x, y)

    reversed = []
    for i in -1..1
      for j in -1..1
        next if i == 0 && j == 0

        reversed.concat get_opposite_cells_until_same_color(x, y, color, i, j)
      end
    end

    raise Exception.new("invalid put") if reversed.empty?

    new_cells = @cells.collect{|row| row.dup }
    new_cells[x][y] = color
    for p in reversed
      new_cells[p[0]][p[1]] = color
    end

    Board.new(new_cells)
  end

  def suggest(color)
    candidates(color).max_by {|p|
      board = put(p[0], p[1], color)
      alphabeta(board, 3, color, color)
    }
  end

  def score(color)
    1.0 / (candidates(rev(color)).size + count(rev(color)))
  end

  def alphabeta(board, depth, me, now, a = 0, b = 1)
    return board.score(me) if depth == 0

    if now == me
      for p in candidates(now)
        board = put(p[0], p[1], now)
        a = [a, alphabeta(board, depth-1, me, rev(now), a, b)].max
        return b if a >= b
      end
      return a
    else
      for p in candidates(now)
        board = put(p[0], p[1], now)
        b = [b, alphabeta(board, depth-1, me, rev(now), a, b)].min
        return a if a >= b
      end
      return b
    end
  end

  def rev(color)
    color == "w" ? "b" : "w"
  end

  def count(color)
    @cells.flatten.select{|c| c == color}.size
  end
end

def result(msg)
  me = $color == "White" ? msg["white"] : msg["black"]
  opp = $color == "White" ? msg["black"] : msg["white"]

  puts msg
  puts me >= opp ? "WIN" : "LOSE"
end

def game(ws, msg)
  board = Board.new(msg["board"]["cells"])
  board.print

  next_turn = msg["board"]["nextTurn"]
  if !next_turn.nil? && $color == next_turn
    selected = board.suggest(short_color($color))
    ws.send(JSON.unparse({action: "put", x: selected[0], y: selected[1]}))
  end
end

ws.on :message do |msg|
  whole = JSON.parse(msg.to_s)
  action = whole["action"]
  msg = whole["body"]

  case action
  when "start"
    puts msg["oppositeName"]
    $color = msg["color"]
    game(ws, msg)
  when "put"
    game(ws, msg)
  when "result"
    result msg
  end
end

ws.on :open do
  puts "connected!"
end

ws.on :close do |e|
  p e
  exit 1
end

ws.on :error do |e|
  p e
end

loop do
end
