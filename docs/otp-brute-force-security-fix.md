# OTP brute-force security fix

## Root cause

The generic registration/login OTP service already stored attempts, expiry, resend state, and locks in PostgreSQL and used a pessimistic write lock. Two gaps remained:

1. An active verification lock was checked by Verify but not by Send/Resend, allowing a new challenge to be requested before `blockedUntil` elapsed.
2. Controllers returned a generic verification error without a stable response code, so clients could not reliably react to expiry, fifth-attempt invalidation, reuse, resend limits, or temporary blocking.

The separate password-reset OTP flow also threw exceptions after updating failed attempts without opting those exceptions out of transaction rollback. In a real transactional request, that could undo the attempt increment. Its request throttling was process-local and its `BLOCKED` state had no expiry timestamp.

## Backend implementation

- Maximum five server-side failed attempts per OTP.
- Fifth failure clears the OTP hash, stores a 15-minute lock, and returns `OTP_ATTEMPTS_EXCEEDED`.
- Further Send, Resend, and Verify calls are rejected while the lock is active.
- OTP validity is five minutes and checked before comparison.
- Successful verification clears the OTP hash immediately; reuse returns `OTP_ALREADY_USED`.
- Resend uses a newly generated value, clears attempts, invalidates the previous hash, and observes a 30-second cooldown.
- Send/resend and verification limits use atomic PostgreSQL rate-limit buckets keyed by reference/recipient plus client IP. Password reset now uses the same persistent store.
- Generic and password-reset verification use pessimistic database row locking. Failure exceptions are configured not to roll back attempt/expiry/block state.
- OTPs are generated with `SecureRandom`; only hashes are stored and full OTP values are not logged or returned by APIs.

## API response contract

Generic OTP responses now include `success`, `code`, `message`, `remainingAttempts`, `retryAfterSeconds`, `lockSecondsRemaining`, and expiry metadata. Codes include:

- `OTP_SENT`
- `OTP_VERIFIED`
- `INVALID_OTP`
- `OTP_EXPIRED`
- `OTP_ATTEMPTS_EXCEEDED`
- `OTP_ALREADY_USED`
- `OTP_NOT_FOUND`
- `OTP_RESEND_COOLDOWN`
- `OTP_RESEND_LIMIT_EXCEEDED`
- `OTP_TEMPORARILY_BLOCKED`
- `OTP_RATE_LIMITED`

Rate-limit and temporary-block responses use HTTP 429. Password-reset mobile errors expose equivalent codes through the existing mobile error envelope and `Retry-After` header.

## Frontend implementation

- OTP inputs accept only six digits.
- Verify stays disabled until six digits are present and while a request is in progress.
- Server-provided remaining attempts and response codes drive messages and control state.
- Expiry countdowns are display-only; the backend remains authoritative.
- Expired, consumed, missing, or attempt-exhausted OTPs are cleared and cannot be submitted.
- Resend clears the previous input and restarts expiry/cooldown displays from backend timing data.
- Temporary blocks disable OTP operations until the server-provided retry period ends.
- Browser refresh, DOM edits, and direct API clients cannot reset database attempt or lock state.

## Database changes

Existing generic tables are reused:

- `otp_verification_state`
- `otp_rate_limit_bucket`

Password reset is extended by [V108__password_reset_otp_lock_support.java](../maharecruitment-web/src/main/java/db/postmigration/V108__password_reset_otp_lock_support.java), with reference SQL in [V108__password_reset_otp_lock_support.sql](sql/V108__password_reset_otp_lock_support.sql).

## Modified implementation files

Backend:

- `DatabaseOtpVerificationService.java`
- `OtpVerificationController.java`
- `OtpLoginController.java`
- `OtpRateLimiter.java`
- `PersistentOtpRateLimitStore.java`
- `OtpVerificationResult.java`
- `VerificationResponse.java`
- `OtpFailureReason.java`
- `OtpRateLimitException.java`
- `OtpResponseCodes.java` (new)
- `PasswordResetServiceImpl.java`
- `PasswordResetRateLimiter.java`
- `PasswordResetRequestEntity.java`
- `PasswordResetProperties.java`
- `OtpAttemptsExceededException.java`
- `OtpTemporarilyBlockedException.java` (new)
- `PasswordResetPageController.java`
- `V108__password_reset_otp_lock_support.java` (new)

Frontend:

- `otp-verification.js`
- `login-otp.js`
- `department-registration.html`
- `login.html`
- `verify-password-reset-otp.html`

## Mandatory VAPT verification matrix

| Test | Expected result |
|---|---|
| TC01 Generate OTP | 200 with `OTP_SENT`; response contains no OTP. |
| TC02 Correct OTP | 200 with `OTP_VERIFIED`. |
| TC03 Incorrect OTP | Database counter increments. |
| TC04 Remaining attempts | Backend returns 4, 3, 2, 1, then 0. |
| TC05 Fifth failure | Hash cleared and `OTP_ATTEMPTS_EXCEEDED`. |
| TC06 Sixth attempt | Rejected before OTP comparison. |
| TC07 Correct value after limit | Rejected while hash remains null. |
| TC08 Expired OTP | `OTP_EXPIRED`, including for the correct value. |
| TC09 Reuse | `OTP_ALREADY_USED`/invalid request state. |
| TC10 Resend value | New value differs from previous value. |
| TC11 Previous value after resend | Rejected. |
| TC12 Resend attempt state | Failed counter resets to zero. |
| TC13 Resend burst | HTTP 429 with retry timing. |
| TC14 Temporary lock | Send/Resend/Verify all rejected until expiry. |
| TC15 Browser refresh | Database counters and locks are unchanged. |
| TC16 DOM button manipulation | Backend still rejects incomplete/blocked requests. |
| TC17 Postman/Burp/curl | Same backend counters and limits apply. |
| TC18 Parallel verify | Pessimistic row lock serializes requests; one OTP cannot be consumed twice. |
| TC19 Immediate consumption | Successful verification clears the stored OTP hash. |
| TC20 Frontend response handling | Codes drive inline attempts, expiry, resend, and block messages. |

For manual VAPT, retain the same session cookie, issue repeated `/verify` requests, inspect the fifth and sixth responses, then try the correct value and `/send` during lockout. Repeat resend requests against the same email/mobile from both one IP and multiple sessions. Confirm HTTP 429 and that application restart or another node does not reset `otp_rate_limit_bucket`.
