# API reference (current starter)

Base URL: `http://localhost:8080/api`. JSON requests and responses. All API routes except registration and login require a bearer token.

## Authentication and roles

Register with `POST /auth/register` or sign in with `POST /auth/login`. Both return `{ "token": "...", "user": { "email": "...", "role": "CITIZEN" } }`. Passwords must be at least 12 characters and no more than 72 UTF-8 bytes. Send `Authorization: Bearer <token>` on protected requests. Tokens expire after eight hours; sign in again to get a fresh token. `GET /auth/me` returns the current account.

Public registration always creates the `CITIZEN` role. Configure `ADMIN_EMAIL` and `ADMIN_PASSWORD` at initial startup to create the first administrator. Admins can list accounts with `GET /admin/users` and set a role with `PUT /admin/users/{id}/role`, body `{ "role": "COLLECTOR" }`. Roles are `CITIZEN`, `COLLECTOR`, `RECYCLER`, and `ADMIN`; an admin cannot remove their own admin role. Role changes require users to sign in again.

| Role | Item access |
|---|---|
| `CITIZEN` | Create submissions; list/view only items owned by their account |
| `COLLECTOR` | Read all items and create submissions |
| `RECYCLER` | Read all items |
| `ADMIN` | Read all items, create submissions, manage account roles |

Existing items from before migration V2 have no owner and remain accessible only to staff roles.

## `GET /items`

Returns records visible to the current role, with trace history. Citizens receive only their own submissions.

## `POST /items`

Creates a submission owned by the signed-in account and an initial `SUBMITTED` trace event. Only citizens, collectors, and admins can create records.

```json
{
  "name": "Office laptop",
  "category": "Computers and laptops",
  "weightKg": 2.4,
  "address": "Demo address",
  "description": "Retired device"
}
```

`name` (1–120 chars), `category` (1–80), `weightKg` (0.01–100000), and `address` (1–300) are required. `description` is optional (up to 1000 chars). Validation errors use Spring's standard 400 response; missing IDs return 404.

## `GET /items/{id}`

Returns the item, assignment details, scheduled pickup time, and trace events. Each event includes previous/new status, actor, notes, and timestamp.

## `POST /items/{id}/lifecycle`

Applies one lifecycle action. The API checks the current status, account role, assignment, and required data in a transaction. Concurrent updates to one item are serialized. A successful response is the updated item with its new history event. Invalid transitions return `409`; unauthorized roles or assignees return `403`.

| Action | Required current status | Permitted role | Extra fields / result |
|---|---|---|---|
| `SCHEDULE_PICKUP` | `SUBMITTED` | Collector or admin | Future `scheduledAt` (within 90 days). Collector assigns self. Admin also supplies `collectorEmail`. Moves to `PICKUP_SCHEDULED`. |
| `CONFIRM_COLLECTION` | `PICKUP_SCHEDULED` | Assigned collector or admin | Moves to `COLLECTED`. |
| `SUBMIT_FOR_VERIFICATION` | `COLLECTED` | Assigned collector or admin | Moves to `AWAITING_VERIFICATION`. |
| `ASSIGN_RECYCLER` | `AWAITING_VERIFICATION` | Admin | `recyclerEmail` must belong to a recycler. Moves to `RECYCLER_ASSIGNED`. |
| `VERIFY` | `RECYCLER_ASSIGNED` | Assigned recycler or admin | `verified: true` moves to `VERIFIED`; `false` requires a reason and moves to terminal `REJECTED`. |
| `START_RECYCLING` | `VERIFIED` | Assigned recycler or admin | Moves to `RECYCLING`. |
| `COMPLETE_RECYCLING` | `RECYCLING` | Assigned recycler or admin | Moves to terminal `RECYCLED`. |
| `SAFE_DISPOSAL` | `VERIFIED` | Assigned recycler or admin | Required `notes` describe the method. Moves to terminal `DISPOSED`. |

Example request to schedule a pickup as a collector:

```json
{
  "action": "SCHEDULE_PICKUP",
  "scheduledAt": "2026-10-20T09:30:00Z",
  "notes": "Customer requested a morning visit"
}
```

Admin scheduling also includes `"collectorEmail": "collector@example.com"`. Recycler assignment includes `"recyclerEmail": "recycler@example.com"`. Lifecycle notes are optional except when rejecting an item or recording safe disposal; notes may be up to 600 characters. All successful changes append an audit event with the acting account and timestamp. Current item status names describe the workflow; this starter does not yet store uploaded evidence, recovery quantities, facility details, or disposal certificates.

## Notifications

Notifications are in-app only; email and SMS delivery are not configured. The inbox is always scoped to the bearer token's account.

- `GET /notifications` returns the most recent 50 notifications plus an `unreadCount` across the account's inbox.
- `PATCH /notifications/{id}/read` marks one notification read. A notification belonging to another account is returned as `404`.
- `PATCH /notifications/read-all` marks all of the signed-in account's unread notifications as read.

New submissions notify their owner. Lifecycle transitions notify the item owner; scheduling and recycler assignment also notify the assigned staff account.

## Reports and export

- `GET /reports/summary` returns item count, total weight, recycled/disposed counts, totals by status, and totals by category.
- `GET /reports/export` downloads the matching records as CSV.

Both endpoints accept optional `from=YYYY-MM-DD`, `to=YYYY-MM-DD`, and `status=<ItemStatus>` query parameters. Date boundaries use UTC calendar days, and `to` must be on or after `from`. Citizens see only their own items; staff roles see all items. The CSV contains trace ID, category, weight, status, and submission timestamp. It omits pickup address, description, and account email, and escapes spreadsheet formula prefixes in text cells. Report output contains current status and submitted weight; it is not a material recovery or disposal certificate.

## Health

`GET /actuator/health` is served outside the `/api` prefix.
