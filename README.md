# scalachess.js

scalachess.js is a chess library that runs in a webworker, with multi-variants support.

It is based on the awesome [scalachess](https://github.com/ornicar/scalachess) library
compiled to JavaScript, thanks to [Scala.js](https://www.scala-js.org/).

It is currently used in production in [lichess.org](http://lichess.org) mobile
application. So you can see a [real world usage](https://github.com/veloce/lichobile/blob/master/src/chess.ts) on [the mobile app repo](https://github.com/veloce/lichobile).

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

## Build

    $ git submodule update --init
    $ ./build.sh

Generated file will be in `build` dir.

## API

### Message Format

The same format is used for requests and responses:

```typescript
interface ScalachessMessage {
  topic: string
  payload?: Object
  reqid?: string
}
```

The worker will always reply with the same `topic` field of the request.
`payload` is either request arguments or response data. The optional `reqid`
can be used to differentiate responses if you are sending a lot of requests with
the same topic at the same time.

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
* `pgnRead` parse a PGN string a returns the whole game history
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
  },
  reqid: '1'
});
```

Response:
```js
{
    "reqid": "1",
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

```js

var pgn = '[Event "Fischer - Spassky World Championship Match"]\n' +
'[Site "Reykjavik ISL"]\n' +
'[Date "1972.08.22"]\n' +
'[EventDate "?"]\n' +
'[Round "17"]\n' +
'[Result "1/2-1/2"]\n' +
'[White "Boris Spassky"]\n' +
'[Black "Robert James Fischer"]\n' +
'[ECO "B09"]\n' +
'[WhiteElo "?"]\n' +
'[BlackElo "?"]\n' +
'[PlyCount "89"]\n' +
'\n' +
'1. e4 d6 2. d4 g6 3. Nc3 Nf6 4. f4 Bg7 5. Nf3 c5 6. dxc5 Qa5\n' +
'7. Bd3 Qxc5 8. Qe2 O-O 9. Be3 Qa5 10. O-O Bg4 11. Rad1 Nc6\n' +
'12. Bc4 Nh5 13. Bb3 Bxc3 14. bxc3 Qxc3 15. f5 Nf6 16. h3 Bxf3\n' +
'17. Qxf3 Na5 18. Rd3 Qc7 19. Bh6 Nxb3 20. cxb3 Qc5+ 21. Kh1\n' +
'Qe5 22. Bxf8 Rxf8 23. Re3 Rc8 24. fxg6 hxg6 25. Qf4 Qxf4\n' +
'26. Rxf4 Nd7 27. Rf2 Ne5 28. Kh2 Rc1 29. Ree2 Nc6 30. Rc2 Re1\n' +
'31. Rfe2 Ra1 32. Kg3 Kg7 33. Rcd2 Rf1 34. Rf2 Re1 35. Rfe2 Rf1\n' +
'36. Re3 a6 37. Rc3 Re1 38. Rc4 Rf1 39. Rdc2 Ra1 40. Rf2 Re1\n' +
'41. Rfc2 g5 42. Rc1 Re2 43. R1c2 Re1 44. Rc1 Re2 45. R1c2\n' +
'1/2-1/2';

worker.postMessage({
  topic: 'pgnRead',
  payload: {
    pgn: pgn
  }
});
```

Response

```js
{
    "replay": [
        {
            "check": false,
            "checkCount": {
                "black": 0,
                "white": 0
            },
            "dests": {
                "a2": [ "a3", "a4" ], "b1": [ "a3", "c3" ], "b2": [ "b3", "b4" ], "c2": [ "c3", "c4" ], "d2": [ "d3", "d4" ], "e2": [ "e3", "e4" ], "f2": [ "f3", "f4" ], "g1": [ "f3", "h3" ], "g2": [ "g3", "g4" ], "h2": [ "h3", "h4" ]
            },
            "end": false,
            "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "pgnMoves": [],
            "playable": true,
            "player": "white",
            "ply": 0,
            "uciMoves": [],
            "variant": "standard"
        },
        {
            "check": false,
            "checkCount": {
                "black": 0,
                "white": 0
            },
            "dests": {
                "a7": [ "a6", "a5" ], "b7": [ "b6", "b5" ], "b8": [ "a6", "c6" ], "c7": [ "c6", "c5" ], "d7": [ "d6", "d5" ], "e7": [ "e6", "e5" ], "f7": [ "f6", "f5" ], "g7": [ "g6", "g5" ], "g8": [ "f6", "h6" ], "h7": [ "h6", "h5" ]
            },
            "end": false,
            "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
            "pgnMoves": [
                "e4"
            ],
            "playable": true,
            "player": "black",
            "ply": 1,
            "uciMoves": [
                "e2e4"
            ],
            "variant": "standard"
        },
        {
            "check": false,
            "checkCount": {
                "black": 0,
                "white": 0
            },
            "dests": {
                "a2": [ "a3", "a4" ], "b1": [ "a3", "c3" ], "b2": [ "b3", "b4" ], "c2": [ "c3", "c4" ], "d1": [ "e2", "f3", "g4", "h5" ], "d2": [ "d3", "d4" ], "e1": [ "e2" ], "e4": [ "e5" ], "f1": [ "e2", "d3", "c4", "b5", "a6" ], "f2": [ "f3", "f4" ], "g1": [ "f3", "h3", "e2" ], "g2": [ "g3", "g4" ], "h2": [ "h3", "h4" ]
            },
            "end": false,
            "fen": "rnbqkbnr/ppp1pppp/3p4/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            "pgnMoves": [
                "e4",
                "d6"
            ],
            "playable": true,
            "player": "white",
            "ply": 2,
            "uciMoves": [
                "d7d6"
            ],
            "variant": "standard"
        },
        {
            "check": false,
            "checkCount": {
                "black": 0,
                "white": 0
            },
            "dests": {
                "a7": [ "a6", "a5" ], "b7": [ "b6", "b5" ], "b8": [ "d7", "a6", "c6" ], "c7": [ "c6", "c5" ], "c8": [ "d7", "e6", "f5", "g4", "h3" ], "d6": [ "d5" ], "d8": [ "d7" ], "e7": [ "e6", "e5" ], "e8": [ "d7" ], "f7": [ "f6", "f5" ], "g7": [ "g6", "g5" ], "g8": [ "f6", "h6" ], "h7": [ "h6", "h5" ]
            },
            "end": false,
            "fen": "rnbqkbnr/ppp1pppp/3p4/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2",
            "pgnMoves": [
                "e4",
                "d6",
                "d4"
            ],
            "playable": true,
            "player": "black",
            "ply": 3,
            "uciMoves": [
                "d2d4"
            ],
            "variant": "standard"
        },
        ... // till up to ply 89
    ],
    "setup": {
        "check": false,
        "checkCount": {
            "black": 0,
            "white": 0
        },
        "dests": {
            "a2": [ "a3", "a4" ], "b1": [ "a3", "c3" ], "b2": [ "b3", "b4" ], "c2": [ "c3", "c4" ], "d2": [ "d3", "d4" ], "e2": [ "e3", "e4" ], "f2": [ "f3", "f4" ], "g1": [ "f3", "h3" ], "g2": [ "g3", "g4" ], "h2": [ "h3", "h4" ]
        },
        "end": false,
        "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "pgnMoves": [],
        "playable": true,
        "player": "white",
        "ply": 0,
        "uciMoves": [],
        "variant": "standard"
    },
    "variant": {
        "key": "standard",
        "name": "Standard",
        "shortName": "Std",
        "title": "Standard rules of chess (FIDE)"
    }
}
```
