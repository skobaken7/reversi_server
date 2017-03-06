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
  def initialize(cells, color)
    @cells = cells
    @color = short_color(color)
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
end

def game(ws, msg)
  board = Board.new(msg["board"]["cells"], $color)
  board.print

  next_turn = msg["board"]["nextTurn"]
  if !next_turn.nil? && $color == next_turn
    selected = board.candidates(short_color($color))[0]
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
    pp msg
  when "timeout"
    puts "#{msg["color"]} is timeout."
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
