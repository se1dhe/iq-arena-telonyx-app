# WebSocket Events

## Client -> Server

```json
{"type":"ping","payload":{}}
```

```json
{"type":"queue.join","payload":{"mode":"ranked_duel"}}
```

```json
{"type":"queue.leave","payload":{}}
```

```json
{"type":"round.answer","payload":{"matchId":"uuid","round":1,"selectedIndex":0}}
```

## Server -> Client

```json
{"type":"welcome","payload":{"playerId":"uuid","serverTime":"..."}}
```

```json
{"type":"queue.status","payload":{"status":"searching"}}
```

```json
{"type":"round.open","payload":{"matchId":"uuid","round":1,"prompt":"...","options":["A","B","C","D"]}}
```

```json
{"type":"round.reveal","payload":{"correctIndex":0,"explanation":"..."}}
```
