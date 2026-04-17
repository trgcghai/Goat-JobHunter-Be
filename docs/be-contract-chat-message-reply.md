# BE Contract - Chat Message Reply

## 1) Scope

Tai lieu nay mo ta ro contract backend cho tinh nang reply message, gom:

- Payload send message (reply va khong reply)
- Payload response tra ve (reply va khong reply)
- Payload realtime qua STOMP/WebSocket (reply va khong reply)
- Cac thay doi can FE cap nhat

Ap dung cho chat room da ton tai.

## 2) Thay doi contract (quan trong)

### 2.1 Request send message

- Them field moi: replyToMessageId (optional)

### 2.2 Response message

- Field cu: replyTo
- Field moi thay the: replyToMessageId
- Field moi bo sung: replyContext

### 2.3 Breaking change cho FE

- FE phai doi doc field tu replyTo sang replyToMessageId.
- FE phai render context reply tu replyContext (khong tu content goc trong client cache nua).

### 2.4 Bang thay doi payload (old -> new)

| Luong                    | Truoc day | Hien tai                              |
| ------------------------ | --------- | ------------------------------------- |
| Send request             | content   | content + replyToMessageId (optional) |
| Message response         | replyTo   | replyToMessageId + replyContext       |
| Realtime message payload | replyTo   | replyToMessageId + replyContext       |

## 3) Endpoint va channel lien quan

### REST

- POST /api/v1/chatrooms/{id}/messages
- GET /api/v1/chatrooms/{id}/messages
- GET /api/v1/chatrooms/{id}/media
- GET /api/v1/chatrooms/{id}/file

### Realtime

- Subscribe STOMP topic: /topic/chatrooms/{chatRoomId}
- WebSocket endpoint: /ws (SockJS)

## 4) Send message payload

## 4.1 Text message - khong reply

Request (application/json):

```json
{
  "content": "Xin chao"
}
```

Hoac:

```json
{
  "content": "Xin chao",
  "replyToMessageId": null
}
```

## 4.2 Text message - co reply

Request (application/json):

```json
{
  "content": "Minh dong y",
  "replyToMessageId": "msg_2f8a8d9b"
}
```

## 4.3 Media message - khong reply

Request (multipart/form-data):

- files: 1..n file
- request (optional):

```json
{
  "content": "Caption optional"
}
```

## 4.4 Media message - co reply

Request (multipart/form-data):

- files: 1..n file
- request (bat buoc neu can reply):

```json
{
  "content": "Caption optional",
  "replyToMessageId": "msg_2f8a8d9b"
}
```

## 4.5 Validation

Neu co replyToMessageId:

- Message goc phai ton tai
- Message goc phai cung conversation (cung chatRoom)
- Neu khong hop le: HTTP 400, message:

```json
{
  "statusCode": 400,
  "message": "replyToMessageId is invalid or not in this conversation",
  "error": "Exception Occurred"
}
```

Luu y:

- Message goc da thu hoi (hidden) van duoc phep reply.

## 5) Response payload (MessageResponse)

Luu y cho endpoint send:

- POST /api/v1/chatrooms/{id}/messages tra ve List<MessageResponse>
- Text-only se tra list co 1 phan tu
- Files-only hoac files+text co the tra list nhieu phan tu

Schema:

```json
{
  "messageId": "msg_xxx",
  "chatRoomId": "123",
  "sender": {
    "accountId": 10,
    "fullName": "Nguyen Van A",
    "username": "vana",
    "email": "a@example.com",
    "avatar": "https://..."
  },
  "content": "Noi dung",
  "messageType": "TEXT",

  "replyToMessageId": "msg_parent_or_null",
  "replyContext": {
    "originalMessageId": "msg_parent",
    "originalSender": {
      "accountId": 11,
      "fullName": "Tran Thi B",
      "username": "thib",
      "email": "b@example.com",
      "avatar": "https://..."
    },
    "originalMessageType": "TEXT",
    "originalContentPreview": "Noi dung preview ngan...",
    "originalMessageUnavailable": false,
    "originalMessageHidden": false
  },

  "isHidden": false,
  "isForwarded": false,
  "originalMessageId": null,
  "createdAt": "2026-04-14T12:00:00Z",
  "updatedAt": "2026-04-14T12:00:00Z"
}
```

Luu y:

- originalMessageId (root level) la field cu cho forward message, khac voi replyContext.originalMessageId.

## 5.1 Response - khong reply

```json
{
  "messageId": "msg_no_reply",
  "chatRoomId": "123",
  "sender": { "accountId": 10 },
  "content": "Tin nhan thuong",
  "messageType": "TEXT",
  "replyToMessageId": null,
  "replyContext": null,
  "isHidden": false,
  "isForwarded": false,
  "originalMessageId": null,
  "createdAt": "2026-04-14T12:00:00Z",
  "updatedAt": "2026-04-14T12:00:00Z"
}
```

## 5.2 Response - reply, message goc con ton tai

```json
{
  "messageId": "msg_reply_1",
  "chatRoomId": "123",
  "sender": { "accountId": 10 },
  "content": "Reply day",
  "messageType": "TEXT",
  "replyToMessageId": "msg_parent_1",
  "replyContext": {
    "originalMessageId": "msg_parent_1",
    "originalSender": { "accountId": 11 },
    "originalMessageType": "TEXT",
    "originalContentPreview": "Noi dung message goc...",
    "originalMessageUnavailable": false,
    "originalMessageHidden": false
  },
  "isHidden": false,
  "isForwarded": false,
  "originalMessageId": null,
  "createdAt": "2026-04-14T12:01:00Z",
  "updatedAt": "2026-04-14T12:01:00Z"
}
```

## 5.3 Response - reply, message goc da thu hoi

```json
{
  "messageId": "msg_reply_2",
  "chatRoomId": "123",
  "sender": { "accountId": 10 },
  "content": "Reply vao message da thu hoi",
  "messageType": "TEXT",
  "replyToMessageId": "msg_parent_hidden",
  "replyContext": {
    "originalMessageId": "msg_parent_hidden",
    "originalSender": { "accountId": 11 },
    "originalMessageType": "TEXT",
    "originalContentPreview": "Tin nhắn đã được thu hồi",
    "originalMessageUnavailable": false,
    "originalMessageHidden": true
  },
  "isHidden": false,
  "isForwarded": false,
  "originalMessageId": null,
  "createdAt": "2026-04-14T12:02:00Z",
  "updatedAt": "2026-04-14T12:02:00Z"
}
```

## 5.4 Response - reply, message goc khong kha dung (bi xoa cung)

```json
{
  "messageId": "msg_reply_3",
  "chatRoomId": "123",
  "sender": { "accountId": 10 },
  "content": "Reply cu",
  "messageType": "TEXT",
  "replyToMessageId": "msg_parent_deleted",
  "replyContext": {
    "originalMessageId": "msg_parent_deleted",
    "originalSender": null,
    "originalMessageType": null,
    "originalContentPreview": "Tin nhắn không khả dụng",
    "originalMessageUnavailable": true,
    "originalMessageHidden": false
  },
  "isHidden": false,
  "isForwarded": false,
  "originalMessageId": null,
  "createdAt": "2026-04-14T12:03:00Z",
  "updatedAt": "2026-04-14T12:03:00Z"
}
```

## 6) Realtime payload

## 6.1 Message event (new message/revoked message)

Backend publish vao:

- /topic/chatrooms/{chatRoomId}

Payload dung cung schema MessageResponse (muc 5).

### Realtime - khong reply

```json
{
  "messageId": "msg_rt_1",
  "chatRoomId": "123",
  "messageType": "TEXT",
  "content": "Tin moi",
  "replyToMessageId": null,
  "replyContext": null,
  "isHidden": false
}
```

### Realtime - co reply

```json
{
  "messageId": "msg_rt_2",
  "chatRoomId": "123",
  "messageType": "TEXT",
  "content": "Reply realtime",
  "replyToMessageId": "msg_parent_rt",
  "replyContext": {
    "originalMessageId": "msg_parent_rt",
    "originalSender": { "accountId": 11 },
    "originalMessageType": "IMAGE",
    "originalContentPreview": "image",
    "originalMessageUnavailable": false,
    "originalMessageHidden": false
  },
  "isHidden": false
}
```

## 6.2 Permanent delete event (khong doi)

Channel /topic/chatrooms/{chatRoomId} van co the nhan event xoa cung:

```json
{
  "eventType": "MESSAGE_DELETED",
  "chatRoomId": "123",
  "messageId": "msg_deleted",
  "deletedByAccountId": 10,
  "deletedAt": "2026-04-14T12:10:00Z"
}
```

FE can parse payload theo eventType de phan biet MessageResponse va MessageDeletedEventResponse.

## 7) Huong dan FE migrate nhanh

1. Thay toan bo doc field replyTo -> replyToMessageId.
2. Neu replyToMessageId != null thi render block quote tu replyContext.
3. Neu replyContext.originalMessageUnavailable = true thi hien thi "Tin nhắn không khả dụng".
4. Neu replyContext.originalMessageHidden = true thi hien thi "Tin nhắn đã được thu hồi".
5. Click vao preview dung replyContext.originalMessageId de scroll/focus message goc neu con ton tai tren UI list.
6. Luon null-safe voi cac field replyContext (du lieu cu khong reply se null).

## 8) Backward compatibility

- Du lieu cu khong co reply relation van hoat dong binh thuong:
  - replyToMessageId = null
  - replyContext = null
- Khong can migration schema DB cho thay doi nay.
