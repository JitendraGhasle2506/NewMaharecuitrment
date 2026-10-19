# Audit client IP capture

New persistent audit records capture a client IP through one shared request filter,
request context and JPA insert listener. IPv4 and IPv6 literals are accepted without
DNS lookup. The context is cleared in `finally` and is not inherited by pooled threads.
Async and error servlet dispatches re-establish their own context.

Coverage: shared `audit_event` (including OTP/password-reset and workflow timelines),
login/logout/failure history, asset audit, agency master, commission rate, MAHAIT
profile, team management, employee type conversion, cell mapping and location mapping.
These are the application's ten dedicated audit tables. Ordinary created/updated
timestamps on business entities are not separate audit histories.

In HTTP requests the captured IP overrides caller-supplied IP fields and `clientIp`
metadata. By default it comes from the connection address. Existing setting
`app.security.transport.trust-forwarded-headers=true` allows the first valid
`X-Forwarded-For` address, then `X-Real-IP`, with the connection address as fallback.
Only enable this setting behind a proxy that removes/replaces incoming forwarding
headers and prevents direct access to the application. This change does not enable it.

Non-request system work stores `SYSTEM` when no explicitly captured IP is supplied;
an unavailable connection address stores `UNKNOWN`. Async workers that need to retain
an originating request's IP must capture it before dispatch and provide it explicitly.
Do not infer a user's IP from a later request or invent an address for scheduled work.

Migration **V155** idempotently adds nullable `ip_address varchar(64)` columns without
rewriting historical records. Existing asset/authentication IP columns are retained.
The shared audit timeline DTO now includes `ipAddress`. Resource mapping, reassignment
and release use the shared audit trail with before/after snapshots and transactional
rollback if the audit write fails. No audit history is sent through the resource list API.
