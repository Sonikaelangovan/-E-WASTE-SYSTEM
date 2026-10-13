# API reference (current starter)

Base URL: `http://localhost:8080/api`. JSON requests and responses. No authentication or authorization is currently implemented; keep this API local and do not use real personal information.

## `GET /items`

Returns persisted items, newest/oldest ordering as supplied by JPA, with trace history.

## `POST /items`

Creates a submission and initial `SUBMITTED` trace event.

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

Returns the item and its trace events. The event includes previous/new status, actor, notes, and timestamp. In the current API, only the initial event can be created; there is no public status-transition endpoint.

## Health

`GET /actuator/health` is served outside the `/api` prefix.
