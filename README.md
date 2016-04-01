# scalachessjs

Port of the awesome [scalachess](https://github.com/ornicar/scalachess) to
JavaScript, thanks to [Scala.js](https://www.scala-js.org/).

## Features

* Fully asynchronous: runs in a web worker, it doesn't block your UI while you're
  computing chess logic.
* Multi variants support: Chess 960, King Of The Hill, Three-check, Antichess,
Atomic chess, Horde, Racing Kings, Crazyhouse!

## API

### Message Format

The same format is used for questions and answers:

```js
{
  topic: [String],
  payload: [Object]
}
```

The library will always reply with the same `topic` field of the request.
`payload` is in either case request's args or response's data.

### Topics

* `init`: initialize the board with a givent variant
* `dests`: get possible destinations for a given position
* `move`: play a move for a given position
* `step`: a variation of the `move` topic for game analysis purpose

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
    "dests": {
        "a2": [
            "a3",
            "a4"
        ],
        "b2": [
            "b3",
            "b4"
        ],
        "c2": [
            "c3",
            "c4"
        ],
        "d1": [
            "c3",
            "e3"
        ],
        "d2": [
            "d3",
            "d4"
        ],
        "e2": [
            "e3",
            "e4"
        ],
        "f1": [
            "e3",
            "g3"
        ],
        "f2": [
            "f3",
            "f4"
        ],
        "g2": [
            "g3",
            "g4"
        ],
        "h2": [
            "h3",
            "h4"
        ]
    },
    "fen": "qbrnbnkr/pppppppp/8/8/8/8/PPPPPPPP/QBRNBNKR w KQkq - 0 1",
    "player": "white",
    "variant": {
        "key": "chess960",
        "name": "Chess960",
        "shortName": "960",
        "title": "Starting position of the home rank pieces is randomized."
    }
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
    },
    "variant": "kingOfTheHill"
}
```

#### Move

Request:

```js
chessWorker.postMessage({
  topic: 'move',
  payload: {
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    variant: 'threeCheck',
    orig: 'e2',
    dest: 'e3'
  }
});
```

Response:
```js
{
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
    "fen": "rnbqkbnr/pppppppp/8/8/8/4P3/PPPP1PPP/RNBQKBNR b KQkq - 0 1 +0+0",
    "lastMove": [
        "e2",
        "e3"
    ],
    "playable": true,
    "player": "black",
    "ply": 1,
    "san": "e3",
    "uci": "e2e3",
    "variant": "threeCheck"
}
```
