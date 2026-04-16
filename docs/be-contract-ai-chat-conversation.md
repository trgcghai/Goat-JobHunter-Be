# BE Contract - AI Chat + Conversation

## 1) Scope

Tai lieu nay mo ta contract backend cho module AI, gom:

- AI chat theo conversation
- Quan ly conversation (tao, sua title, pin/unpin, xoa)
- Lay danh sach conversation co phan trang
- Lay danh sach tin nhan AI trong conversation co phan trang
- API sinh tags cho blog

## 2) Base Path

Tat ca endpoint trong tai lieu nay dung base path:

- `/api/v1/ai`

## 3) Auth / Session

Luu y quan trong cho FE:

- He thong doc token tu cookie `accessToken` (khong doc tu Authorization header trong flow hien tai).
- Voi API can current user (`/chat`, `/conversations/**`), neu khong co user se tra 400:

```json
{
  "statusCode": 400,
  "message": "User not authenticated",
  "error": "Exception Occurred"
}
```

Khuyen nghi FE:

- Goi API kem credentials (vd `credentials: "include"`).

## 4) Response Envelope

### 4.1 Wrapper mac dinh

Da so endpoint tra theo envelope:

```json
{
  "statusCode": 200,
  "message": "Success hoac ApiMessage",
  "data": {},
  "error": null
}
```

### 4.2 Exception quan trong

```json
{
  "statusCode": 400,
  "message": "...",
  "error": "Exception Occurred"
}
```

Validation error:

```json
{
  "statusCode": 400,
  "message": "Title must not be blank",
  "error": "Invalid request content."
}
```

### 4.3 Truong hop dac biet

- `POST /api/v1/ai/chat` tra ve **raw string** (khong wrap envelope) vi response body la String.

## 5) DTO Summary

## 5.1 Request DTO

### ChatRequest

```json
{
  "conversationId": 123,
  "message": "Toi muon tim viec backend Java"
}
```

Validation:

- `conversationId`: required
- `message`: required, not blank

### BlogContentRequest

```json
{
  "content": "Noi dung bai viet..."
}
```

Validation:

- `content`: required, not blank

### ConversationCreateRequest

```json
{
  "title": "Career advice"
}
```

Luu y:

- Request body co the bo trong.
- Neu title null/blank, backend set mac dinh: `New conversation`.

### ConversationTitleUpdateRequest

```json
{
  "title": "CV review round 2"
}
```

Validation:

- `title`: required, not blank

### ConversationPinUpdateRequest

```jsonadmin@gmail.com
12345678

conghai.tpma@gmail.com
Nguyễn Đức Hải
0123123123
TPHCM
NgDucHai123!

conghai.tpma@gmail.com
fedorasky215@gmail.com
haitruong.tpma@gmail.com
weeboo.unused030@aleeas.com

12345678x@X
987654321x@X

FRIEND_REQUEST

nguyenthikhanhnhi789000@gmail.com

lập plan để thực hiện phần chat AI + AI message, tạo 2 entity mới là conversation và aiMessage, chỉnh sửa lại service và implement của ai để nối vào 2 entity đó
các model của conversation và aiMessage có nối relationship với account bằng annotation, viết xml để thực hiện và tạo.
tạo các endpoint để thực hiện như sau: tạo conversation mới, đổi tiêu đề conversation, ghim / bỏ ghim conversation, xóa conversation
{
  "pinned": true
}
```

Validation:

- `pinned`: required

## 5.2 Response DTO (data payload)

### ConversationResponse

```json
{
  "conversationId": 123,
  "title": "Career advice",
  "pinned": false,
  "createdAt": "2026-04-16T10:00:00Z",
  "updatedAt": "2026-04-16T10:05:00Z"
}
```

### ConversationPinnedResponse

```json
{
  "conversationId": 123,
  "pinned": true
}
```

### AIMessageResponse

```json
{
  "aiMessageId": 456,
  "role": "USER",
  "content": "Toi muon toi uu CV",
  "createdAt": "2026-04-16T10:06:00Z",
  "updatedAt": "2026-04-16T10:06:00Z"
}
```

`role` enum:

- `USER`
- `AI`

### ResultPaginationResponse

```json
{
  "meta": {
    "page": 1,
    "pageSize": 10,
    "pages": 3,
    "total": 25
  },
  "result": []
}
```

Luu y:

- He thong dung one-indexed page cho request (`page=1` la trang dau).
- Gia tri `meta.page` cung la one-indexed.

## 6) API Detail

## 6.1 Chat voi AI

### POST `/api/v1/ai/chat`

Request body: `ChatRequest`

Response: raw string

```text
Ban co the toi uu CV bang cach...
```

Error thuong gap:

- 400 `Conversation ID is required`
- 400 `Message is required` / `Message is not empty`
- 400 `Conversation not found or access denied`
- 400 `User not authenticated`

## 6.2 Generate blog tags

### POST `/api/v1/ai/generate/blogs/tags`

Request body: `BlogContentRequest`

Response (wrapped):

```json
{
  "statusCode": 200,
  "message": "Success",
  "data": ["Java", "Spring Boot", "Backend"],
  "error": null
}
```

Validation error:

- 400 `Content is required`

## 6.3 Tao conversation

### POST `/api/v1/ai/conversations`

Request body: `ConversationCreateRequest` (optional)

Response (wrapped, 201):

```json
{
  "statusCode": 201,
  "message": "Conversation created",
  "data": {
    "conversationId": 123,
    "title": "New conversation",
    "pinned": false,
    "createdAt": "2026-04-16T10:00:00Z",
    "updatedAt": null
  },
  "error": null
}
```

## 6.4 Doi title conversation

### PATCH `/api/v1/ai/conversations/{conversationId}/title`

Path param:

- `conversationId`: Long

Request body: `ConversationTitleUpdateRequest`

Response (wrapped):

```json
{
  "statusCode": 200,
  "message": "Conversation title updated",
  "data": {
    "conversationId": 123,
    "title": "CV review",
    "pinned": false,
    "createdAt": "2026-04-16T10:00:00Z",
    "updatedAt": "2026-04-16T10:10:00Z"
  },
  "error": null
}
```

## 6.5 Pin / Unpin conversation

### PATCH `/api/v1/ai/conversations/{conversationId}/pin`

Path param:

- `conversationId`: Long

Request body: `ConversationPinUpdateRequest`

Response (wrapped):

```json
{
  "statusCode": 200,
  "message": "Conversation pin status updated",
  "data": {
    "conversationId": 123,
    "pinned": true
  },
  "error": null
}
```

## 6.6 Xoa conversation

### DELETE `/api/v1/ai/conversations/{conversationId}`

Path param:

- `conversationId`: Long

Response (wrapped):

```json
{
  "statusCode": 200,
  "message": "Conversation deleted",
  "data": null,
  "error": null
}
```

## 6.7 Lay conversation cua toi (phan trang)

### GET `/api/v1/ai/conversations?page=1&size=10`

Query params:

- `page`: one-indexed
- `size`: so phan tu moi trang
- Co the gui them `sort`, nhung backend dang uu tien sort query noi bo theo pinned/updatedAt

Response (wrapped):

```json
{
  "statusCode": 200,
  "message": "Conversation list fetched",
  "data": {
    "meta": {
      "page": 1,
      "pageSize": 10,
      "pages": 2,
      "total": 12
    },
    "result": [
      {
        "conversationId": 123,
        "title": "Career advice",
        "pinned": true,
        "createdAt": "2026-04-16T10:00:00Z",
        "updatedAt": "2026-04-16T10:20:00Z"
      }
    ]
  },
  "error": null
}
```

## 6.8 Lay messages trong conversation (phan trang)

### GET `/api/v1/ai/conversations/{conversationId}/messages?page=1&size=20`

Path param:

- `conversationId`: Long

Query params:

- `page`: one-indexed
- `size`: so phan tu moi trang

Response (wrapped):

```json
{
  "statusCode": 200,
  "message": "Conversation messages fetched",
  "data": {
    "meta": {
      "page": 1,
      "pageSize": 20,
      "pages": 1,
      "total": 2
    },
    "result": [
      {
        "aiMessageId": 456,
        "role": "AI",
        "content": "Ban nen bo sung muc du an...",
        "createdAt": "2026-04-16T10:06:10Z",
        "updatedAt": "2026-04-16T10:06:10Z"
      },
      {
        "aiMessageId": 455,
        "role": "USER",
        "content": "Toi muon toi uu CV",
        "createdAt": "2026-04-16T10:06:00Z",
        "updatedAt": "2026-04-16T10:06:00Z"
      }
    ]
  },
  "error": null
}
```

Error thuong gap:

- 400 `Conversation ID is required`
- 400 `Conversation not found or access denied`
- 400 `User not authenticated`

## 7) FE Integration Notes

- Voi endpoint tra raw string (`/chat`), FE parse nhu text thuong.
- Voi endpoint con lai, FE parse theo envelope `statusCode/message/data/error`.
- Voi endpoint phan trang, du lieu can dung nam trong `data.result`, metadata nam trong `data.meta`.
- Danh sach message dang tra theo thu tu moi nhat truoc (DESC theo createdAt/coalesce).
