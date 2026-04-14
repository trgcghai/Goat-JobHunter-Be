# BE Contract - User Block / Unblock

## 1) Scope

This document defines backend contracts used by frontend for:

- Block user
- Unblock user
- Realtime sync for block/unblock status

Applies to the current implementation in this project.

## 2) Authentication

All endpoints below require authenticated user context.

## 3) REST Endpoints

### 3.1 Block User

- Method: `POST`
- URL: `/api/v1/friend-requests/{id}/block`
- Path param:
  - `id` (Long): target user account id
- Request body: none

Success response:

- Status: `200 OK`
- Content-Type: `application/json`
- Body schema (FriendRequestResponse):

```json
{
  "requestId": null,
  "senderId": 123,
  "receiverId": 456,
  "status": null,
  "relationshipState": "BLOCKED",
  "requestedAt": "2026-04-13T09:10:11.123Z",
  "respondedAt": null
}
```

Notes:

- `senderId` = actor (current user)
- `receiverId` = target user
- Blocking upserts relationship to `BLOCKED`
- If currently `FRIEND`, it is overwritten to `BLOCKED`
- All pending friend requests for this pair are canceled

### 3.2 Unblock User

- Method: `POST`
- URL: `/api/v1/friend-requests/{id}/unblock`
- Path param:
  - `id` (Long): target user account id
- Request body: none

Success response:

- Status: `200 OK`
- Content-Type: `application/json`
- Body schema (FriendRequestResponse):

```json
{
  "requestId": null,
  "senderId": 123,
  "receiverId": 456,
  "status": null,
  "relationshipState": "NONE",
  "requestedAt": "2026-04-13T09:20:21.123Z",
  "respondedAt": null
}
```

Notes:

- Unblock removes `BLOCKED` row from `user_relationships` (hard delete)
- If no blocked row exists, response is still success (`NONE`)

## 4) Error Contract (important for FE mapping)

Blocked interaction error:

- HTTP status: `403 FORBIDDEN`
- Error code: `BLOCKED_INTERACTION`
- Response envelope (RestResponse):

```json
{
  "statusCode": 403,
  "message": "Cannot send friend request because this pair is blocked",
  "data": null,
  "error": "BLOCKED_INTERACTION"
}
```

Common validation errors:

- `400 BAD_REQUEST` for invalid input (example: self block/unblock, invalid id)
- `404 NOT_FOUND` when target user does not exist

## 5) Realtime Contract

## 5.1 Channel

User-specific STOMP destination:

- Subscribe from FE: `/user/queue/friendships`

Server sends to both users in the pair (actor and target) after successful transaction commit.

## 5.2 Event Types

For block/unblock:

- `USER_BLOCKED`
- `USER_UNBLOCKED`

## 5.3 Event Payload Schema

Payload type: `FriendshipRealtimeEventResponse`

```json
{
  "type": "USER_BLOCKED",
  "actorUser": {
    "accountId": 123,
    "fullName": "Actor Name",
    "username": "actor_username",
    "avatar": "https://...",
    "headline": "...",
    "bio": "...",
    "coverPhoto": "https://...",
    "visibility": "PUBLIC"
  },
  "targetUser": {
    "accountId": 456,
    "fullName": "Target Name",
    "username": "target_username",
    "avatar": "https://...",
    "headline": "...",
    "bio": "...",
    "coverPhoto": "https://...",
    "visibility": "PUBLIC"
  },
  "requestId": null,
  "relationshipState": "BLOCKED",
  "emittedAt": "2026-04-13T09:10:11.456Z"
}
```

For unblock event:

```json
{
  "type": "USER_UNBLOCKED",
  "actorUser": { "accountId": 123 },
  "targetUser": { "accountId": 456 },
  "requestId": null,
  "relationshipState": "NONE",
  "emittedAt": "2026-04-13T09:20:21.456Z"
}
```

## 6) FE Behavior Recommendations

When receiving `USER_BLOCKED`:

- Disable direct chat input between actor/target
- Hide/disable add friend actions for this pair
- Refresh profile action button state for this pair

When receiving `USER_UNBLOCKED`:

- Re-enable pair actions according to normal non-friend state
- Allow sending friend request again

## 7) Related Backend Effects (for FE expectations)

While pair is `BLOCKED`:

- Cannot send friend request
- Cannot accept friend request
- Cannot create direct chat with that user
- Cannot send new message in existing direct chat with that user
- `/api/v1/users/search` excludes users blocked in either direction

## 8) Quick Test Checklist for FE

- Call block endpoint and verify:
  - `200 OK`
  - realtime `USER_BLOCKED` received on `/user/queue/friendships`
- Call unblock endpoint and verify:
  - `200 OK`
  - realtime `USER_UNBLOCKED` received only when previous blocked relationship existed
- Try friend request or direct message while blocked:
  - `403` + `BLOCKED_INTERACTION`
