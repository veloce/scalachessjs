# scalachessjs

Port of the awesome [scalachess](https://github.com/ornicar/scalachess) library
to JavaScript, thanks to [Scala.js](https://www.scala-js.org/).

## Features

* Fully asynchronous: runs in a web worker, it does not block your UI while you're
  computing chess logic.
* Completely stateless: you send the complete game position in each request,
either with FEN or PGN
* Built from extensively tested code: the scalachess library is by itself well tested
  and powers all the chess logic of the thousands of games being played
  every day on [lichess.org](http://lichess.org).
* Multi variants support: Chess 960, King Of The Hill, Three-check, Antichess,
Atomic chess, Horde, Racing Kings, Crazyhouse!

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
* `move` will play a move for a given position set by FEN; you can optionally
  pass a `pgnMoves` array so you can accumulate moves. Useful for other requests
  like `threefoldTest`. Pass an `uciMoves` array if you want to accumulate moves
  in the UCI format
* `threefoldTest` test if current situation falls under the threefold repetition
  rule. Warning: it can be slow since it has to replay the whole game from the
  PGN moves
* `pgnRead` parse a PGN string a returns a situation object
* `pgnDump` takes an initial FEN, a list of moves and returns a formatted PGN
  string

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
              "a2": [ "a3", "a4" ], "b2": [ "b3", "b4" ], "c1": [ "b3", "d3" ], "c2": [ "c3", "c4" ], "d2": [ "d3", "d4" ], "e2": [ "e3", "e4" ], "f2": [ "f3", "f4" ], "g2": [ "g3", "g4" ], "h1": [ "g3" ], "h2": [ "h3", "h4" ]
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
            "a2": [ "a3", "a4" ], "b1": [ "a3", "c3" ], "b2": [ "b3", "b4" ], "c2": [ "c3", "c4" ], "d2": [ "d3", "d4" ], "e2": [ "e3", "e4" ], "f2": [ "f3", "f4" ], "g1": [ "f3", "h3" ], "g2": [ "g3", "g4" ], "h2": [ "h3", "h4" ]
        }
    },
    "topic": "dests"
}
```

#### Move

Request:

```js
worker.postMessage({
  topic: 'move',
  payload: {
    fen: 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2 +0+0',
    variant: 'threeCheck',
    pgnMoves: ['e4', 'e5'],
    uciMoves: ['e2e4', 'e7e5'],
    orig: 'd2',
    dest: 'd4',
    path: '0'
  }
});
```

Response:
```js
{
    "path": "0",
    "situation": {
        "check": false,
        "checkCount": {
            "black": 0,
            "white": 0
        },
        "dests": {
            "a7": [ "a6", "a5" ], "b7": [ "b6", "b5" ], "b8": [ "a6", "c6" ], "c7": [ "c6", "c5" ], "d7": [ "d6", "d5" ], "d8": [ "e7", "f6", "g5", "h4" ], "e5": [ "d4" ], "e8": [ "e7" ], "f7": [ "f6", "f5" ], "f8": [ "e7", "d6", "c5", "b4", "a3" ], "g7": [ "g6", "g5" ], "g8": [ "e7", "f6", "h6" ], "h7": [ "h6", "h5" ]
        },
        "end": false,
        "fen": "rnbqkbnr/pppp1ppp/8/4p3/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2 +0+0",
        "pgnMoves": [
            "e4", "e5", "d4"
        ],
        "playable": true,
        "player": "black",
        "ply": 3,
        "uciMoves": [
            "e2e4", "e7e5", "d2d4"
        ],
        "variant": "threeCheck"
    }
}
```


#### pgnRead

Request

Response

```js
{
    "situation": {
        "check": false,
        "checkCount": {
            "black": 0,
            "white": 0
        },
        "dests": {
            "a6": [ "a5" ], "b7": [ "b6", "b5" ], "c6": [ "b8", "d8", "a7", "a5", "e5", "b4", "d4" ], "d6": [ "d5" ], "e2": [ "e3", "e4", "e1", "d2", "c2", "f2", "g2" ], "e7": [ "e6", "e5" ], "f7": [ "f6", "f5" ], "g5": [ "g4" ], "g7": [ "g8", "g6", "h7", "f8", "h8", "f6", "h6" ]
        },
        "end": false,
        "fen": "8/1p2ppk1/p1np4/6p1/2R1P3/1P4KP/P1R1r1P1/8 b - - 7 45",
        "pgnMoves": [
            "e4", "d6", "d4", "g6", "Nc3", "Nf6", "f4", "Bg7", "Nf3", "c5", "dxc5", "Qa5", "Bd3", "Qxc5", "Qe2", "O-O", "Be3", "Qa5", "O-O", "Bg4", "Rad1", "Nc6", "Bc4", "Nh5", "Bb3", "Bxc3", "bxc3", "Qxc3", "f5", "Nf6", "h3", "Bxf3", "Qxf3", "Na5", "Rd3", "Qc7", "Bh6", "Nxb3", "cxb3", "Qc5+", "Kh1", "Qe5", "Bxf8", "Rxf8", "Re3", "Rc8", "fxg6", "hxg6", "Qf4", "Qxf4", "Rxf4", "Nd7", "Rf2", "Ne5", "Kh2", "Rc1", "Ree2", "Nc6", "Rc2", "Re1", "Rfe2", "Ra1", "Kg3", "Kg7", "Rcd2", "Rf1", "Rf2", "Re1", "Rfe2", "Rf1", "Re3", "a6", "Rc3", "Re1", "Rc4", "Rf1", "Rdc2", "Ra1", "Rf2", "Re1", "Rfc2", "g5", "Rc1", "Re2", "R1c2", "Re1", "Rc1", "Re2", "R1c2"
        ],
        "playable": true,
        "player": "black",
        "ply": 89,
        "uciMoves": [
            "e2e4", "d7d6", "d2d4", "g7g6", "b1c3", "g8f6", "f2f4", "f8g7", "g1f3", "c7c5", "d4c5", "d8a5", "f1d3", "a5c5", "d1e2", "e8h8", "c1e3", "c5a5", "e1h1", "c8g4", "a1d1", "b8c6", "d3c4", "f6h5", "c4b3", "g7c3", "b2c3", "a5c3", "f4f5", "h5f6", "h2h3", "g4f3", "e2f3", "c6a5", "d1d3", "c3c7", "e3h6", "a5b3", "c2b3", "c7c5", "g1h1", "c5e5", "h6f8", "a8f8", "d3e3", "f8c8", "f5g6", "h7g6", "f3f4", "e5f4", "f1f4", "f6d7", "f4f2", "d7e5", "h1h2", "c8c1", "e3e2", "e5c6", "e2c2", "c1e1", "f2e2", "e1a1", "h2g3", "g8g7", "c2d2", "a1f1", "e2f2", "f1e1", "f2e2", "e1f1", "e2e3", "a7a6", "e3c3", "f1e1", "c3c4", "e1f1", "d2c2", "f1a1", "c2f2", "a1e1", "f2c2", "g6g5", "c2c1", "e1e2", "c1c2", "e2e1", "c2c1", "e1e2", "c1c2"
        ],
        "variant": "standard"
    }
}
```
