# BE Contract - Chat Message Search

## 1) Scope

Tai lieu nay mo ta contract backend cho API tim kiem tin nhan theo chat room.

- Tim theo content message
- Chi tim trong 1 chat room
- Loai bo message da thu hoi
- Khong tra message da bi xoa vinh vien
- Yeu cau user phai co quyen truy cap room

## 2) Endpoint

- Method: GET
- Path: /api/v1/chatrooms/{id}/messages/search
- Query params:
  - searchTerm (optional)
  - page, size (optional - theo Pageable)

Vi du:

```http
GET /api/v1/chatrooms/123/messages/search?searchTerm=java&page=1&size=20
```

## 3) Auth va permission

- Yeu cau da dang nhap.
- User phai la member cua chat room.
- Neu khong phai member: HTTP 400 voi message `User is not in chat room`.

## 4) Business rules

### 4.1 Pham vi tim kiem

- Chi tim trong partition message cua chatRoomId duoc truyen vao path.
- Khong tim cross-room.

### 4.2 Match content

- Match theo partial contains.
- Khong phan biet hoa thuong (case-insensitive).

### 4.3 Visibility

- Message da thu hoi (`isHidden=true`) bi loai khoi ket qua.
- Message da bi xoa vinh vien khong ton tai trong datasource, nen khong xuat hien.

### 4.4 searchTerm rong/whitespace

- Neu `searchTerm` null, rong, hoac chi gom khoang trang:
  - HTTP 200
  - `result = []`
  - `meta.total = 0`

### 4.5 Validation

- chatRoomId null: HTTP 400 (`Chat room ID cannot be null`).
- searchTerm dai qua 100 ky tu: HTTP 400.
- size <= 0: fallback default 20.
- size > 50: cap ve 50.

## 5) Response format

API tra ve `ResultPaginationResponse`:

```json
{
  "meta": {
    "page": 1,
    "pageSize": 20,
    "pages": 1,
    "total": 3
  },
  "result": [
    {
      "messageId": "msg_abc",
      "chatRoomId": "123",
      "sender": {
        "accountId": 10,
        "fullName": "Nguyen Van A",
        "username": "vana",
        "email": "a@example.com",
        "avatar": "https://..."
      },
      "content": "java spring",
      "messageType": "TEXT",
      "replyToMessageId": null,
      "replyContext": null,
      "contactCard": null,
      "isHidden": false,
      "isForwarded": false,
      "originalMessageId": null,
      "createdAt": "2026-04-17T10:00:00Z",
      "updatedAt": "2026-04-17T10:00:00Z"
    }
  ]
}
```

## 6) Error response

Theo `GlobalExceptionHandler`, loi nghiep vu tra ve dang `RestResponse`.

Vi du:

```json
{
  "statusCode": 400,
  "message": "searchTerm must not exceed 100 characters",
  "error": "Exception Occurred"
}
```

## 7) Notes cho FE/QA

- Endpoint moi khong thay doi contract endpoint cu:
  - GET /api/v1/chatrooms/{id}/messages
  - GET /api/v1/chatrooms/{id}/media
  - GET /api/v1/chatrooms/{id}/file
- Search duoc toi uu theo scan cap o tang repository de tranh query qua nang.
- Neu room co du lieu rat lon, nen uu tien filter keyword cu the (>= 2-3 ky tu) de latency tot hon.
