# BE Contract - Friendship (Friend Request + Block/Unblock)

## 1. Scope

Tai lieu nay mo ta contract cho FE su dung voi cac action:

- Send friend request
- Accept friend request
- Reject friend request
- Cancel friend request
- Block user
- Unblock user
- Get my blocked users (paginated)
- Realtime event dong bo UI

## 2. Base Information

- REST base path: `/api/v1/friend-requests`
- WebSocket endpoint: `/ws` (SockJS enabled)
- User subscribe channel: `/user/queue/friendships`
- Tat ca endpoint can auth.

## 3. Payload Type Definitions

## 3.1 Request Types

### CreateFriendRequestRequest

```ts
export interface CreateFriendRequestRequest {
  targetUserId: number;
}
```

## 3.2 Response Types

Luu y: `FriendRequestResponse` dang dung `@JsonInclude(NON_NULL)`, cac field null co the bi omit.

### FriendRequestResponse

```ts
export interface FriendRequestResponse {
  requestId?: number;
  senderId?: number;
  receiverId?: number;
  status?: FriendRequestStatus;
  relationshipState?: RelationshipState;
  requestedAt?: string; // ISO-8601
  respondedAt?: string; // ISO-8601
}
```

### FriendUserSnippetResponse

```ts
export interface FriendUserSnippetResponse {
  accountId?: number;
  fullName?: string;
  username?: string;
  avatar?: string;
  headline?: string;
  bio?: string;
  coverPhoto?: string;
  visibility?: Visibility;
}
```

### FriendshipRealtimeEventResponse

```ts
export interface FriendshipRealtimeEventResponse {
  type: FriendshipRealtimeEventType;
  actorUser?: FriendUserSnippetResponse;
  targetUser?: FriendUserSnippetResponse;
  requestId?: number;
  relationshipState?: RelationshipState;
  emittedAt?: string; // ISO-8601
}
```

## 3.3 Enums

### FriendRequestStatus

```ts
export type FriendRequestStatus =
  | "PENDING"
  | "ACCEPTED"
  | "REJECTED"
  | "CANCELED"
  | "EXPIRED";
```

### RelationshipState

```ts
export type RelationshipState = "FRIEND" | "BLOCKED" | "NONE";
```

### FriendshipRealtimeEventType

```ts
export type FriendshipRealtimeEventType =
  | "FRIEND_REQUEST_CREATED"
  | "FRIEND_REQUEST_ACCEPTED"
  | "FRIEND_REQUEST_REJECTED"
  | "FRIEND_REQUEST_CANCELED"
  | "USER_BLOCKED"
  | "USER_UNBLOCKED";
```

## 4. REST API Contract

## 4.1 Send Friend Request

- Method: `POST`
- URL: `/api/v1/friend-requests`
- Body:

```json
{
  "targetUserId": 456
}
```

- Success: `201 CREATED`
- Success payload example:

```json
{
  "requestId": 1001,
  "senderId": 123,
  "receiverId": 456,
  "status": "PENDING",
  "requestedAt": "2026-04-13T10:00:00.000Z"
}
```

## 4.2 Accept Friend Request

- Method: `POST`
- URL: `/api/v1/friend-requests/{id}/accept`
- Path param: `id` = friend request id
- Body: none
- Success: `200 OK`
- Success payload example:

```json
{
  "requestId": 1001,
  "senderId": 123,
  "receiverId": 456,
  "status": "ACCEPTED",
  "relationshipState": "FRIEND",
  "requestedAt": "2026-04-13T10:00:00.000Z",
  "respondedAt": "2026-04-13T10:05:00.000Z"
}
```

## 4.3 Reject Friend Request

- Method: `POST`
- URL: `/api/v1/friend-requests/{id}/reject`
- Path param: `id` = friend request id
- Body: none
- Success: `200 OK`
- Success payload example:

```json
{
  "requestId": 1001,
  "senderId": 123,
  "receiverId": 456,
  "status": "REJECTED",
  "requestedAt": "2026-04-13T10:00:00.000Z",
  "respondedAt": "2026-04-13T10:05:00.000Z"
}
```

## 4.4 Cancel Friend Request

- Method: `POST`
- URL: `/api/v1/friend-requests/{id}/cancel`
- Path param: `id` = friend request id
- Body: none
- Success: `200 OK`
- Success payload example:

```json
{
  "requestId": 1001,
  "senderId": 123,
  "receiverId": 456,
  "status": "CANCELED",
  "requestedAt": "2026-04-13T10:00:00.000Z",
  "respondedAt": "2026-04-13T10:05:00.000Z"
}
```

## 4.5 Block User

- Method: `POST`
- URL: `/api/v1/friend-requests/{id}/block`
- Path param: `id` = target user account id (khong phai request id)
- Body: none
- Success: `200 OK`
- Success payload example:

```json
{
  "senderId": 123,
  "receiverId": 456,
  "relationshipState": "BLOCKED",
  "requestedAt": "2026-04-13T10:10:00.000Z"
}
```

Business side effect:

- Neu dang FRIEND thi overwrite sang BLOCKED
- Cancel toan bo friend request PENDING cua pair

## 4.6 Unblock User

- Method: `POST`
- URL: `/api/v1/friend-requests/{id}/unblock`
- Path param: `id` = target user account id (khong phai request id)
- Body: none
- Success: `200 OK`
- Success payload example:

```json
{
  "senderId": 123,
  "receiverId": 456,
  "relationshipState": "NONE",
  "requestedAt": "2026-04-13T10:20:00.000Z"
}
```

Business side effect:

- Xoa quan he BLOCKED
- Sau do pair ve trang thai chua ket ban

## 4.7 Get My Blocked Users (Paginated)

- Method: `GET`
- URL: `/api/v1/friend-requests/me/block`
- Query params (optional):
  - `page` (1-based from FE perspective via BE meta.page)
  - `size`
  - `sort` (default: `blockedSince,DESC`)
- Body: none
- Success: `200 OK`
- Success payload example:

```json
{
  "meta": {
    "page": 1,
    "pageSize": 20,
    "pages": 1,
    "total": 2
  },
  "result": [
    {
      "accountId": 456,
      "fullName": "Bob",
      "username": "bob",
      "avatar": "https://..."
    },
    {
      "accountId": 789,
      "fullName": "Charlie",
      "username": "charlie",
      "avatar": "https://..."
    }
  ]
}
```

## 5. Realtime Contract

## 5.1 Channel

- FE connect WebSocket: `/ws`
- FE subscribe friendship events: `/user/queue/friendships`

Server emit cho ca actor va target (neu target principal hop le), va chi emit sau khi transaction commit.

## 5.2 Realtime Event Matrix

| Action                | Event type                | requestId    | relationshipState |
| --------------------- | ------------------------- | ------------ | ----------------- |
| Send friend request   | `FRIEND_REQUEST_CREATED`  | request id   | omitted/null      |
| Accept friend request | `FRIEND_REQUEST_ACCEPTED` | request id   | `FRIEND`          |
| Reject friend request | `FRIEND_REQUEST_REJECTED` | request id   | omitted/null      |
| Cancel friend request | `FRIEND_REQUEST_CANCELED` | request id   | omitted/null      |
| Block user            | `USER_BLOCKED`            | omitted/null | `BLOCKED`         |
| Unblock user          | `USER_UNBLOCKED`          | omitted/null | `NONE`            |

## 5.3 Realtime Payload Example

### FRIEND_REQUEST_CREATED

```json
{
  "type": "FRIEND_REQUEST_CREATED",
  "actorUser": {
    "accountId": 123,
    "fullName": "Alice",
    "username": "alice",
    "avatar": "https://..."
  },
  "targetUser": {
    "accountId": 456,
    "fullName": "Bob",
    "username": "bob",
    "avatar": "https://..."
  },
  "requestId": 1001,
  "emittedAt": "2026-04-13T10:00:00.000Z"
}
```

### USER_BLOCKED

```json
{
  "type": "USER_BLOCKED",
  "actorUser": {
    "accountId": 123,
    "fullName": "Alice"
  },
  "targetUser": {
    "accountId": 456,
    "fullName": "Bob"
  },
  "relationshipState": "BLOCKED",
  "emittedAt": "2026-04-13T10:10:00.000Z"
}
```

### USER_UNBLOCKED

```json
{
  "type": "USER_UNBLOCKED",
  "actorUser": {
    "accountId": 123,
    "fullName": "Alice"
  },
  "targetUser": {
    "accountId": 456,
    "fullName": "Bob"
  },
  "relationshipState": "NONE",
  "emittedAt": "2026-04-13T10:20:00.000Z"
}
```

## 6. Error Contract (FE map)

Response envelope:

```json
{
  "statusCode": 403,
  "message": "Cannot send friend request because this pair is blocked",
  "data": null,
  "error": "BLOCKED_INTERACTION"
}
```

Important status codes:

- `400 BAD_REQUEST`: invalid input, invalid actor, business validation fail
- `403 FORBIDDEN`: blocked interaction (`error = BLOCKED_INTERACTION`)
- `404 NOT_FOUND`: user/request not found
- `409 CONFLICT`: state conflict (already friends, pending already exists, request no longer pending)

## 7. FE Notes

- Cung mot endpoint namespace `/friend-requests` dang dung cho ca request actions va block/unblock.
- Param `id` co 2 nghia khac nhau:
  - accept/reject/cancel: friend request id
  - block/unblock: target user account id
- De tranh bug FE, nen tach ro route-level type trong API client.
