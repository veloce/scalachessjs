# scalachessjs

Port of the awesome [scalachess](https://github.com/ornicar/scalachess) library
to JavaScript, thanks to [Scala.js](https://www.scala-js.org/).

## Features

* Fully asynchronous: runs in a web worker, it doesn't block your UI while you're
  computing chess logic.
* Completely stateless: you send the complete game position for each request,
either with FEN or PGN
* Built from extensively tested code: the scala library is by itself well tested
  and it also powers all the chess logic of the thousands of games being played
  every day on [lichess.org](http://lichess.org).
* Multi variants support: Chess 960, King Of The Hill, Three-check, Antichess,
Atomic chess, Horde, Racing Kings, Crazyhouse!

## Performance note

`pgnMove` request can be slow, since the library will parse PGN and replay all
moves to compute a position for each move. You should only use it when you want
to know a game status that takes into account the threefold repetition rule
(which can't be decided with a FEN string only).

`fenMove` is faster and can be used at the same time. It is useful  when you
only want to know a board situation or possible destinations.


## API

### Message Format

The same format is used for requests and responses:

```js
{
  topic: [String],
  payload: [Object]
}
```

The library will always reply with the same `topic` field of the request.
`payload` is either request arguments or response data.

### Topics

* `init` will initialize the board for a given variant
* `dests` will get possible destinations for a given position
* `fenMove` will play a move for a given position set by FEN
* `pgnMove` will play a move for a given position set by an initial FEN and a
list of PGN moves. Slower than `fenMove` but this is the only way to take into
account the draw by threefold repetition rule.

#### Init

Request:

```js
chessWorker.postMessage({
  topic: 'init',
  payload: {
    variant: 'chess960'
  }
});
```

Response:

```js
{
    "payload": {
        "setup": {
            "check": false,
            "dests": {
                "a2": [
                    "a3",
                    "a4"
                ],
                "b2": [
                    "b3",
                    "b4"
                ],
                "c1": [
                    "b3",
                    "d3"
                ],
                "c2": [
                    "c3",
                    "c4"
                ],
                "d2": [
                    "d3",
                    "d4"
                ],
                "e2": [
                    "e3",
                    "e4"
                ],
                "f2": [
                    "f3",
                    "f4"
                ],
                "g2": [
                    "g3",
                    "g4"
                ],
                "h1": [
                    "g3"
                ],
                "h2": [
                    "h3",
                    "h4"
                ]
            },
            "fen": "rbnkbqrn/pppppppp/8/8/8/8/PPPPPPPP/RBNKBQRN w KQkq - 0 1",
            "pgnMoves": [],
            "playable": true,
            "player": "white",
            "ply": 0
        },
        "variant": {
            "key": "chess960",
            "name": "Chess960",
            "shortName": "960",
            "title": "Starting position of the home rank pieces is randomized."
        }
    },
    "topic": "init"
}
```

#### Dests

Request:

```js
chessWorker.postMessage({
  topic: 'dests',
  payload: {
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    variant: 'kingOfTheHill'
  }
});
```

Response:
```js
{
    "payload": {
        "dests": {
            "a2": [
                "a3",
                "a4"
            ],
            "b1": [
                "a3",
                "c3"
            ],
            "b2": [
                "b3",
                "b4"
            ],
            "c2": [
                "c3",
                "c4"
            ],
            "d2": [
                "d3",
                "d4"
            ],
            "e2": [
                "e3",
                "e4"
            ],
            "f2": [
                "f3",
                "f4"
            ],
            "g1": [
                "f3",
                "h3"
            ],
            "g2": [
                "g3",
                "g4"
            ],
            "h2": [
                "h3",
                "h4"
            ]
        }
    },
    "topic": "dests"
}
```

#### FEN move

Request:

```js
chessWorker.postMessage({
  topic: 'fenMove',
  payload: {
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    variant: 'threeCheck',
    orig: 'e2',
    dest: 'e4'
  }
});
```

Response:
```js
{
    "payload": {
        "path": "0",
        "situation": {
            "check": false,
            "dests": {
                "a7": [
                    "a6",
                    "a5"
                ],
                "b7": [
                    "b6",
                    "b5"
                ],
                "b8": [
                    "a6",
                    "c6"
                ],
                "c7": [
                    "c6",
                    "c5"
                ],
                "d7": [
                    "d6",
                    "d5"
                ],
                "e7": [
                    "e6",
                    "e5"
                ],
                "f7": [
                    "f6",
                    "f5"
                ],
                "g7": [
                    "g6",
                    "g5"
                ],
                "g8": [
                    "f6",
                    "h6"
                ],
                "h7": [
                    "h6",
                    "h5"
                ]
            },
            "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1 +0+0",
            "lastMove": {
                "from": "e2",
                "to": "e4",
                "uci": "e2e4"
            },
            "pgnMoves": [
                "e4"
            ],
            "playable": true,
            "player": "black",
            "ply": 1
        },
        "variant": {
            "key": "threeCheck",
            "name": "Three-check",
            "shortName": "3check",
            "title": "Check your opponent 3 times to win the game."
        }
    },
    "topic": "fenMove"
}
```

### PGN move

Request:

```js
chessWorker.postMessage({
  topic: 'pgnMove',
  payload: {
    initialFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    variant: 'standard',
    pgnMoves: ['d4', 'Nf6', 'Nf3', 'Nc6', 'Nbd2', 'e6', 'e4', 'd6', 'c4', 'Qe7', 'Bd3', 'e5', 'd5', 'Nd4', 'Nxd4', 'exd4', 'O-O', 'Bg4', 'f3', 'Bd7', 'Nb3', 'Qe5', 'Be2', 'c5', 'dxc6', 'bxc6', 'Qxd4', 'Qxd4+','Nxd4', 'Rb8', 'b3', 'Be7', 'Be3', 'Bd8', 'Rfd1', 'Bb6', 'Kf2', 'Ke7', 'Rd2', 'Ba5', 'Rd3', 'Bb6', 'Rad1', 'Rhd8', 'g4', 'h6', 'Bf4', 'g5', 'Bg3', 'h5', 'h3', 'h4', 'Bh2', 'Rb7', 'e5', 'dxe5', 'Bxe5', 'Ne8', 'Kg2', 'Bc7', 'Bxc7', 'Rxc7', 'Nf5+','Kf8', 'f4', 'gxf4', 'Nxh4', 'Ng7', 'Bf3', 'Ne6', 'Nf5', 'Nc5', 'Rd4', 'Ne6', 'Rd6', 'c5', 'h4', 'Ng7', 'Nxg7', 'Kxg7', 'g5', 'a5', 'Kf2', 'Kf8', 'Bc6', 'Ke7', 'Ba4', 'Bxa4', 'Rxd8', 'Bc6', 'h5', 'Ke6', 'h6', 'Be4', 'Rh8', 'Re7', 'Re1', 'Kf5', 'h7', 'Kg6', 'Rc8', 'Kxh7', 'Rxc5', 'a4', 'b4', 'Kg6', 'b5', 'f6', 'gxf6', 'Kxf6', 'b6', 'a3', 'Rc7', 'Rxc7', 'bxc7', 'Bb7', 'Re8', 'Kf5', 'c5', 'Ba6', 'Ra8', 'Bb7', 'Rf8+', 'Ke5', 'c6', 'Ba6', 'Ra8', 'Kd6', 'Rxa6', 'Kxc7', 'Kf3', 'Kb8', 'Kxf4', 'Kc8', 'Ke5', 'Kc7', 'Ke6', 'Kd8', 'Kd6', 'Ke8', 'Ra7', 'Kf8', 'c7', 'Kf7', 'c8=Q+', 'Kg6', 'Qg4+','Kf6', 'Ra8', 'Kf7', 'Qf5+', 'Kg7', 'Ra7+', 'Kg8'],
    orig: 'f5',
    dest: 'c8'
  }
});
```

Response:

```js
{
    "payload": {
        "situation": {
            "check": true,
            "fen": "2Q3k1/R7/3K4/8/8/p7/P7/8 b - - 10 78",
            "lastMove": {
                "from": "f5",
                "to": "c8",
                "uci": "f5c8"
            },
            "pgnMoves": [ "d4", "Nf6", "Nf3", "Nc6", "Nbd2", "e6", "e4", "d6", "c4", "Qe7", "Bd3", "e5", "d5", "Nd4", "Nxd4", "exd4", "O-O", "Bg4", "f3", "Bd7", "Nb3", "Qe5", "Be2", "c5", "dxc6", "bxc6", "Qxd4", "Qxd4+", "Nxd4", "Rb8", "b3", "Be7", "Be3", "Bd8", "Rfd1", "Bb6", "Kf2", "Ke7", "Rd2", "Ba5", "Rd3", "Bb6", "Rad1", "Rhd8", "g4", "h6", "Bf4", "g5", "Bg3", "h5", "h3", "h4", "Bh2", "Rb7", "e5", "dxe5", "Bxe5", "Ne8", "Kg2", "Bc7", "Bxc7", "Rxc7", "Nf5+", "Kf8", "f4", "gxf4", "Nxh4", "Ng7", "Bf3", "Ne6", "Nf5", "Nc5", "Rd4", "Ne6", "Rd6", "c5", "h4", "Ng7", "Nxg7", "Kxg7", "g5", "a5", "Kf2", "Kf8", "Bc6", "Ke7", "Ba4", "Bxa4", "Rxd8", "Bc6", "h5", "Ke6", "h6", "Be4", "Rh8", "Re7", "Re1", "Kf5", "h7", "Kg6", "Rc8", "Kxh7", "Rxc5", "a4", "b4", "Kg6", "b5", "f6", "gxf6", "Kxf6", "b6", "a3", "Rc7", "Rxc7", "bxc7", "Bb7", "Re8", "Kf5", "c5", "Ba6", "Ra8", "Bb7", "Rf8+", "Ke5", "c6", "Ba6", "Ra8", "Kd6", "Rxa6", "Kxc7", "Kf3", "Kb8", "Kxf4", "Kc8", "Ke5", "Kc7", "Ke6", "Kd8", "Kd6", "Ke8", "Ra7", "Kf8", "c7", "Kf7", "c8=Q+", "Kg6", "Qg4+", "Kf6", "Ra8", "Kf7", "Qf5+", "Kg7", "Ra7+", "Kg8", "Qc8#" ],
            "playable": false,
            "player": "black",
            "ply": 155,
            "status": {
                "id": 30,
                "name": "mate"
            },
            "winner": "white"
        },
        "variant": {
            "key": "standard",
            "name": "Standard",
            "shortName": "Std",
            "title": "Standard rules of chess (FIDE)"
        }
    },
    "topic": "pgnMove"
}
```
