# Mobile API Documentation

Version: 1.5  
Last updated: 2026-08-11

## 1. Overview

This document describes the mobile APIs used by the Android and iOS applications.

Machine-readable leave-management contract: [OpenAPI 3.1 specification](mobile-leave-api-openapi.yaml).

Base path:

```text
/api/mobile
```

Authentication:

- Login API does not require a token.
- Refresh API does not require a bearer access token; it uses `refreshToken` in the JSON body.
- Logout API does not require a bearer access token; it uses `refreshToken` in the JSON body.
- Forgot-password APIs do not require a bearer access token; the final reset API uses `resetToken` from OTP verification.
- All other mobile APIs require this header:

```http
Authorization: Bearer <accessToken>
```

Default content type:

```http
Content-Type: application/json
```

Multipart APIs use:

```http
Content-Type: multipart/form-data
```

Security rule:

- The authenticated mobile user can access only the active employee profile mapped to that user.
- Pass `employeeId` in protected APIs.
- If the token user does not match the requested `employeeId`, the API returns an authorization error.

Date and time format:

- Dates use ISO format: `yyyy-MM-dd`
- Date-times use ISO format: `yyyy-MM-dd'T'HH:mm:ss`
- Token expiry timestamps use ISO instant format.

## 2. Standard Error Response

All mobile API validation and business errors return this format:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Please provide valid request details.",
  "timestamp": "2026-07-06T12:20:30Z",
  "fieldErrors": [
    {
      "field": "mobileNo",
      "message": "Mobile number must be 10 to 15 digits."
    }
  ]
}
```

Common HTTP status codes:

| Status | Meaning |
| --- | --- |
| 200 | Success |
| 201 | Resource created successfully |
| 400 | Invalid request or validation failed |
| 401 | Missing, invalid, or expired access/refresh token |
| 403 | User is not allowed to access the requested employee |
| 404 | Requested employee, profile, mapping, or application not found |
| 409 | Duplicate data or request conflicts with the resource's current state |
| 500 | Server configuration or unexpected error |

## 3. Endpoint Summary

| Feature | Method | Endpoint | Auth Required |
| --- | --- | --- | --- |
| Login | POST | `/api/mobile/auth/login` | No |
| Refresh access token | POST | `/api/mobile/auth/refresh` | No, uses refresh token body |
| Logout | POST | `/api/mobile/auth/logout` | No, uses refresh token body |
| Request password-reset OTP | POST | `/api/mobile/auth/password-reset/request-otp` | No |
| Verify password-reset OTP | POST | `/api/mobile/auth/password-reset/verify-otp` | No |
| Reset forgotten password | POST | `/api/mobile/auth/password-reset/reset` | No, uses reset token body |
| Get mapped locations | GET | `/api/mobile/employee-locations?employeeId={employeeId}` | Yes |
| Get profile | GET | `/api/mobile/profile?employeeId={employeeId}` | Yes |
| Update email/mobile | PATCH | `/api/mobile/profile/contact` | Yes |
| Update photo | POST | `/api/mobile/profile/photo` | Yes |
| Change password | POST | `/api/mobile/profile/password/change` | Yes |
| Authenticated profile password reset | POST | `/api/mobile/profile/password/reset` | Yes |
| Check in | POST | `/api/mobile/attendance/check-in` | Yes |
| Check out | POST | `/api/mobile/attendance/check-out` | Yes |
| Mark attendance | POST | `/api/mobile/attendance/mark` | Yes |
| Attendance history | GET | `/api/mobile/attendance/history` | Yes |
| Get leave types/categories | GET | `/api/mobile/leaves/options?employeeId={employeeId}` | Yes |
| Submit leave application | POST | `/api/mobile/leaves` | Yes |
| Get employee leave history | GET | `/api/mobile/leaves?employeeId={employeeId}` | Yes |
| Validate comp-off worked date | GET | `/api/mobile/leaves/comp-off/validate` | Yes |
| Cancel pending leave | POST | `/api/mobile/leaves/{leaveId}/cancel` | Yes |
| Get leave approval queues | GET | `/api/mobile/leaves/approvals` | Yes, HOD only |
| Approve leave | POST | `/api/mobile/leaves/approvals/{leaveId}/approve` | Yes, HOD only |
| Reject leave | POST | `/api/mobile/leaves/approvals/{leaveId}/reject` | Yes, HOD only |

## 4. Login

### POST `/api/mobile/auth/login`

Use this API to authenticate the mobile app user and receive a short-lived bearer access token plus a long-lived refresh token.

Request:

```json
{
  "username": "employee@example.com",
  "password": "Password@123"
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `username` | Yes | Max 254 characters |
| `password` | Yes | Max 128 characters |

Success response:

```json
{
  "userId": 101,
  "empId": 20,
  "employeeCode": "EMP-020",
  "name": "Rahul Patil",
  "employeeName": "Rahul Patil",
  "email": "employee@example.com",
  "mobileNo": "9876543210",
  "photoUrl": "/uploads/employee-photo/rahul.jpg",
  "faceData": "[0.0,0.0,0.0]",
  "embedding": "[0.0,0.0,0.0]",
  "designationId": 3,
  "designationName": "Operator",
  "departmentId": 7,
  "departmentName": "Operations",
  "subDepartmentId": 12,
  "subDepartmentName": "Field Team",
  "employeeType": "CONTRACT",
  "reportingManagerId": 44,
  "reportingManagerName": "Manager Name",
  "reportingDepartmentId": 7,
  "reportingDepartmentName": "Operations",
  "roles": [
    "ROLE_EMPLOYEE"
  ],
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "Zkyg7bqkQy6R1iQv9GQ3J_yA6...",
  "expiresIn": 3600,
  "expiresAt": "2026-07-06T13:20:30Z",
  "refreshExpiresIn": 2592000,
  "refreshExpiresAt": "2026-08-05T12:20:30Z",
  "loginAt": "2026-07-06T17:50:00",
  "lastLoginAt": "2026-07-05T18:10:00"
}
```

Mobile app action:

- Store `accessToken`, `refreshToken`, `expiresAt`, and `refreshExpiresAt` in platform secure storage.
- Send it in `Authorization: Bearer <accessToken>` for all protected APIs.
- Refresh the access token before `expiresAt` or after one `401 INVALID_TOKEN` response.
- Replace both stored tokens after every successful refresh response.
- Store `empId`; pass it as `employeeId` in protected APIs.
- Store `faceData` or `embedding` for app-side face verification. This value is read from `employee_master.embedding`.

## 4.1 Refresh Access Token

### POST `/api/mobile/auth/refresh`

Use this API when the access token is close to expiry or when a protected API returns `401 INVALID_TOKEN`.

Request:

```json
{
  "refreshToken": "Zkyg7bqkQy6R1iQv9GQ3J_yA6..."
}
```

Success response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "expiresAt": "2026-07-06T14:20:30Z",
  "refreshToken": "new-refresh-token-value",
  "refreshExpiresIn": 2592000,
  "refreshExpiresAt": "2026-08-05T13:20:30Z"
}
```

Important rules:

- Refresh tokens are one-time-use and rotated by the backend.
- The mobile app must replace the old `refreshToken` with the new one from every refresh response.
- If refresh returns `401 INVALID_REFRESH_TOKEN`, clear the local session and show the login screen.
- Do not repeatedly retry refresh with the same old token.

## 4.2 Logout

### POST `/api/mobile/auth/logout`

Use this API to end the mobile app session by revoking the stored refresh token on the backend. The app should clear the locally stored access token and refresh token after a successful logout.

Request:

```json
{
  "refreshToken": "Zkyg7bqkQy6R1iQv9GQ3J_yA6...",
  "logoutFromAllDevices": false
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `refreshToken` | Yes | Max 512 characters |
| `logoutFromAllDevices` | No | `true` revokes all active mobile refresh tokens for the user |

Success response:

```json
{
  "success": true,
  "message": "Logged out successfully."
}
```

Common errors:

| Code | Meaning |
| --- | --- |
| `INVALID_REFRESH_TOKEN` | Refresh token is invalid or expired |

## 5. Employee Mapped Locations

### GET `/api/mobile/employee-locations?employeeId={employeeId}`

Use this API to get the locations mapped to the authenticated employee.

Headers:

```http
Authorization: Bearer <accessToken>
```

Query parameters:

| Parameter | Required | Description |
| --- | --- | --- |
| `employeeId` | Yes | Employee ID returned by login as `empId` |

Success response:

```json
{
  "success": true,
  "message": "Mapped locations fetched successfully.",
  "employeeId": 20,
  "locations": [
    {
      "locationId": 5,
      "officeName": "Mumbai Office",
      "locationName": "Gate 1",
      "latitude": 19.076,
      "longitude": 72.8777,
      "radiusMeters": 100,
      "displayName": "Mumbai Office - Gate 1"
    }
  ]
}
```

## 6. Profile

### GET `/api/mobile/profile?employeeId={employeeId}`

Use this API to show the latest mobile profile details.

Headers:

```http
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "message": "Mobile profile fetched successfully.",
  "userId": 101,
  "employeeId": 20,
  "employeeCode": "EMP-020",
  "name": "Rahul Patil",
  "email": "employee@example.com",
  "mobileNo": "9876543210",
  "photoUrl": "/uploads/employee-photo/rahul.jpg",
  "faceData": "[0.0,0.0,0.0]",
  "embedding": "[0.0,0.0,0.0]",
  "tokenType": null,
  "accessToken": null,
  "expiresIn": null,
  "expiresAt": null
}
```

### PATCH `/api/mobile/profile/contact`

Use this API to update the user's email address and/or mobile number.

Headers:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "employeeId": 20,
  "email": "new.email@example.com",
  "mobileNo": "9876543211"
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `employeeId` | Yes | Must belong to authenticated user |
| `email` | No | Must be valid email if provided |
| `mobileNo` | No | Must be 10 to 15 digits if provided |

Business rules:

- At least one value should be changed.
- Email must be unique.
- Mobile number must be unique.
- Updated values are synchronized with user, employee, and onboarding profile records.
- This API returns a refreshed token after contact update. The mobile app should replace the old token with the new one.

Success response:

```json
{
  "success": true,
  "message": "Mobile profile updated successfully.",
  "userId": 101,
  "employeeId": 20,
  "employeeCode": "EMP-020",
  "name": "Rahul Patil",
  "email": "new.email@example.com",
  "mobileNo": "9876543211",
  "photoUrl": "/uploads/employee-photo/rahul.jpg",
  "faceData": "[0.0,0.0,0.0]",
  "embedding": "[0.0,0.0,0.0]",
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "expiresAt": "2026-07-06T13:20:30Z"
}
```

Common errors:

| Code | Meaning |
| --- | --- |
| `INVALID_EMAIL` | Email format is invalid |
| `EMAIL_ALREADY_EXISTS` | Email is already registered |
| `INVALID_MOBILE` | Mobile number format is invalid |
| `MOBILE_ALREADY_EXISTS` | Mobile number is already registered |
| `PROFILE_UPDATE_NOT_MODIFIED` | No changed value was provided |

### POST `/api/mobile/profile/photo`

Use this API to update the employee profile photo.

Multipart headers:

```http
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data
```

Multipart form fields:

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `employeeId` | Yes | Number | Employee ID |
| `photo` | Yes | File | Profile photo file |
| `embedding` | No | Text | Face/photo embedding text to store in `employee_master.embedding` |
| `faceData` | No | Text | Alias for `embedding` |
| `faceEmbedding` | No | Text | Alias for `embedding` |

JSON headers:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

JSON request:

```json
{
  "employeeId": 20,
  "photo": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
  "embedding": "[0.0,0.0,0.0]"
}
```

JSON fields:

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `employeeId` | Yes | Number | Employee ID |
| `photo` | Yes | Text | Base64 image data. Data URI format is supported. |
| `photoFileName` | No | Text | Optional file name. Defaults to `profile-photo.jpg` or `profile-photo.png`. |
| `photoContentType` | No | Text | Required only when `photo` is plain Base64 without a data URI. Allowed: `image/jpeg`, `image/png`. |
| `embedding` | No | Text | Face/photo embedding text to store in `employee_master.embedding`. Aliases: `faceData`, `faceEmbedding`, `embeddingData`. |

Success response:

```json
{
  "success": true,
  "message": "Profile photo updated successfully.",
  "userId": 101,
  "employeeId": 20,
  "employeeCode": "EMP-020",
  "name": "Rahul Patil",
  "email": "employee@example.com",
  "mobileNo": "9876543210",
  "photoUrl": "/uploads/employee-photo/new-photo.jpg",
  "faceData": "[0.0,0.0,0.0]",
  "embedding": "[0.0,0.0,0.0]",
  "tokenType": null,
  "accessToken": null,
  "expiresIn": null,
  "expiresAt": null
}
```

Common errors:

| Code | Meaning |
| --- | --- |
| `PHOTO_REQUIRED` | Photo file was not provided |
| `ONBOARDING_PROFILE_NOT_FOUND` | Employee onboarding profile is missing |
| `INVALID_IMAGE` | File could not be stored or validated |

## 7. Password

### POST `/api/mobile/profile/password/change`

Use this API when the logged-in mobile user wants to change the password.

Headers:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "employeeId": 20,
  "currentPassword": "OldPass@123",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `employeeId` | Yes | Must belong to authenticated user |
| `currentPassword` | Yes | Must match existing password |
| `newPassword` | Yes | Must follow application password policy |
| `confirmPassword` | Yes | Must match `newPassword` |

Success response:

```json
{
  "success": true,
  "message": "Password changed successfully.",
  "userId": 101,
  "employeeId": 20
}
```

Common errors:

| Code | Meaning |
| --- | --- |
| `CURRENT_PASSWORD_INVALID` | Current password is incorrect |
| `PASSWORD_CONFIRMATION_MISMATCH` | New password and confirm password do not match |
| `INVALID_PASSWORD` | New password does not match policy |
| `PASSWORD_REUSE_NOT_ALLOWED` | New password is same as current password |

### POST `/api/mobile/profile/password/reset`

Use this API for authenticated profile password reset from the mobile app.

Important:

- This endpoint currently requires `currentPassword`.
- For "forgot password" without current password, use the OTP/token flow under `/api/mobile/auth/password-reset/**`.

Headers:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request:

```json
{
  "employeeId": 20,
  "currentPassword": "OldPass@123",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

Success response:

```json
{
  "success": true,
  "message": "Password reset successfully.",
  "userId": 101,
  "employeeId": 20
}
```

## 8. Forgot Password

The mobile forgot-password flow is public and uses OTP verification before allowing a new password to be set.

Default local URL:

```text
http://localhost:8777/maharecruitment/api/mobile/auth/password-reset
```

Headers:

```http
Content-Type: application/json
```

No `Authorization` header is required for any forgot-password endpoint.

Supported identifier values:

- Email address
- Mobile number, 10 to 15 digits
- Employee code or username

Configured limits:

| Setting | Value |
| --- | --- |
| OTP validity | 5 minutes |
| Reset token validity | 10 minutes |
| OTP max attempts | 5 |
| OTP resend cooldown | 60 seconds |
| Max OTP requests | 3 requests per 15 minutes |
| OTP verify rate limit | 10 attempts per 60 seconds |

### POST `/api/mobile/auth/password-reset/request-otp`

Use this API to send an OTP to the registered password-reset delivery channel.

Request:

```json
{
  "identifier": "EMP000033"
}
```

Success response is generic for both existing and non-existing accounts:

```json
{
  "success": true,
  "message": "If the account information is valid, an OTP has been sent to the registered contact details.",
  "resetToken": null,
  "expiresInSeconds": null,
  "maskedDestination": null
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `identifier` | Yes | Username, email, mobile number, or employee code. Maximum 255 characters. |

### POST `/api/mobile/auth/password-reset/verify-otp`

Use this API to verify the OTP. On success, the backend returns a single-use reset token.

Request:

```json
{
  "identifier": "EMP000033",
  "otp": "123456"
}
```

Success response:

```json
{
  "success": true,
  "message": "OTP verified successfully. Use the reset token to set a new password.",
  "resetToken": "secure-single-use-token",
  "expiresInSeconds": 600,
  "maskedDestination": null
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `identifier` | Yes | Same identifier used for request OTP. Maximum 255 characters. |
| `otp` | Yes | Exactly 6 digits. |

### POST `/api/mobile/auth/password-reset/reset`

Use this API to set the new password after OTP verification.

Request:

```json
{
  "resetToken": "secure-single-use-token",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

Success response:

```json
{
  "success": true,
  "message": "Password reset successfully.",
  "resetToken": null,
  "expiresInSeconds": null,
  "maskedDestination": null
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `resetToken` | Yes | Reset token returned by `/verify-otp`. Maximum 512 characters. |
| `newPassword` | Yes | 8 to 100 characters. |
| `confirmPassword` | Yes | Must match `newPassword`. Maximum 100 characters. |

Password policy:

- Must contain at least one uppercase letter.
- Must contain at least one lowercase letter.
- Must contain at least one number.
- Must contain at least one special character.
- Must not start or end with spaces.
- Must not contain spaces.
- Must be different from the current password.
- Must not contain the username, email local part, display name, or employee code.

Common errors:

| HTTP Status | Code | Meaning |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | Required field is missing or request values are invalid |
| 400 | `INVALID_IDENTIFIER` | Identifier is blank or invalid |
| 400 | `INVALID_OTP` | OTP is incorrect |
| 400 | `OTP_EXPIRED` | OTP expired |
| 400 | `PASSWORD_CONFIRMATION_MISMATCH` | New password and confirm password do not match |
| 400 | `PASSWORD_POLICY_FAILED` | New password does not match policy |
| 400 | `PASSWORD_REUSED` | New password matches the current password |
| 401 | `INVALID_RESET_TOKEN` | Reset token is invalid or already used |
| 401 | `RESET_TOKEN_EXPIRED` | Reset token expired |
| 429 | `MAX_ATTEMPTS_EXCEEDED` | Too many invalid OTP attempts |
| 429 | `RATE_LIMIT_EXCEEDED` | Too many reset requests; response includes `Retry-After` header |

## 9. Attendance

Attendance APIs support both multipart image upload and JSON Base64 image upload.

Allowed JSON image formats:

- Raw Base64 string.
- Data URI format: `data:image/jpeg;base64,<base64>` or `data:image/png;base64,<base64>`.

Allowed image content types for JSON upload:

- `image/jpeg`
- `image/png`

#### Server-side attendance update integration

After a mobile punch is committed locally, the portal sends an HTTP `POST` to
`https://mahaitattendance.espltestingsite.in/api/third-party/update-attendance`
with `Content-Type: application/json`. The mobile app does not call this
third-party endpoint directly.

Both attendance integrations use the single configured base URL
`attendance.integration.internal.base-url`; the portal appends the appropriate
fixed endpoint path for report retrieval or mobile attendance updates.

For check-in, the portal sends exactly these fields:

```json
{
  "employee_code": "MahaIT0693",
  "date": "2026-08-11",
  "in_time": "10:00"
}
```

The check-in payload does not contain `out_time`.

For check-out, the portal sends exactly these fields:

```json
{
  "employee_code": "MahaIT0693",
  "date": "2026-08-11",
  "out_time": "11:01"
}
```

The check-out payload does not contain `in_time`. Dates use `yyyy-MM-dd` and
times use 24-hour `HH:mm` format. If the third-party service is unavailable,
the committed local mobile punch is retained and the integration failure is
logged for operations.

The portal applies an earliest-IN/latest-OUT rule across mobile and biometric
punches. A mobile check-in is sent to the update endpoint only when it is
earlier than the existing biometric punches. A mobile check-out is sent only
when it is later than the existing biometric punches. Non-boundary mobile
punches remain stored locally as source events but do not overwrite the
effective upstream IN or OUT time.

Example:

| Time | Source | Treatment |
| --- | --- | --- |
| `09:42` | Mobile App | IN time |
| `09:45` | Biometric | Intermediate entry |
| `13:15` | Biometric | Intermediate entry |
| `17:55` | Mobile App | Intermediate entry |
| `18:10` | Biometric | OUT time |

Effective attendance: `IN 09:42 | OUT 18:10`.

### POST `/api/mobile/attendance/check-in`

Use this API to mark check-in.

Headers:

```http
Authorization: Bearer <accessToken>
```

JSON request:

```json
{
  "employeeId": 20,
  "latitude": 19.076,
  "longitude": 72.8777,
  "locationAddress": "Mumbai Office Gate 1",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
  "imageFileName": "check-in.jpg",
  "imageContentType": "image/jpeg"
}
```

Multipart form fields:

| Field | Required | Type |
| --- | --- | --- |
| `employeeId` | Yes | Number |
| `latitude` | Yes | Decimal |
| `longitude` | Yes | Decimal |
| `locationAddress` | No | Text |
| `image` | Yes | File |

Success response:

```json
{
  "success": true,
  "message": "Check-in marked successfully.",
  "attendanceId": 501,
  "employeeId": 20,
  "employeeCode": "EMP-020",
  "attendanceDate": "2026-07-06",
  "checkInTime": "09:35:00",
  "checkOutTime": null,
  "attendanceSource": "MOBILE_APP",
  "mobileAppStatus": "Y",
  "apiStatus": "N"
}
```

### POST `/api/mobile/attendance/check-out`

Use this API to mark check-out.

The employee may call this endpoint multiple times on the same day. Each successful
call updates the same attendance row with the latest check-out time, location,
image, and total hours while preserving the original check-in.

Headers:

```http
Authorization: Bearer <accessToken>
```

JSON request:

```json
{
  "employeeId": 20,
  "latitude": 19.076,
  "longitude": 72.8777,
  "locationAddress": "Mumbai Office Gate 1",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
  "imageFileName": "check-out.jpg",
  "imageContentType": "image/jpeg"
}
```

Multipart fields are the same as check-in.

The common `/api/mobile/attendance/mark` endpoint has the same behavior when
`attendanceFlag` is `CHECK_OUT`, `CHECKOUT`, or `OUT`.

Success response:

```json
{
  "success": true,
  "message": "Check-out recorded successfully.",
  "attendanceId": 501,
  "employeeId": 20,
  "employeeCode": "EMP-020",
  "attendanceDate": "2026-07-06",
  "checkInTime": "09:35:00",
  "checkOutTime": "18:10:00",
  "attendanceSource": "MOBILE_APP",
  "mobileAppStatus": "Y",
  "apiStatus": "N"
}
```

For the second and subsequent check-outs on the same day, the response message is
`Check-out updated successfully.` and the `attendanceId` remains unchanged.

### POST `/api/mobile/attendance/mark`

Use this API when the app wants one common endpoint for check-in and check-out.

Headers:

```http
Authorization: Bearer <accessToken>
```

JSON request:

```json
{
  "attendanceFlag": "CHECK_IN",
  "employeeId": 20,
  "latitude": 19.076,
  "longitude": 72.8777,
  "locationAddress": "Mumbai Office Gate 1",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
  "imageFileName": "attendance.jpg",
  "imageContentType": "image/jpeg"
}
```

Accepted attendance flag values:

- `CHECK_IN`
- `CHECK_OUT`
- `CHECKIN`
- `CHECKOUT`
- `IN`
- `OUT`

Multipart form fields:

| Field | Required | Type |
| --- | --- | --- |
| `attendanceFlag` | No | Text |
| `flag` | No | Text |
| `employeeId` | Yes | Number |
| `latitude` | Yes | Decimal |
| `longitude` | Yes | Decimal |
| `locationAddress` | No | Text |
| `image` | Yes | File |

Note:

- For multipart, send either `attendanceFlag` or `flag`.

### GET `/api/mobile/attendance/history`

Use this API to show attendance history.

Headers:

```http
Authorization: Bearer <accessToken>
```

Query options:

| Parameter | Required | Description |
| --- | --- | --- |
| `employeeId` | Yes | Employee ID |
| `date` | No | Single date in `yyyy-MM-dd` format |
| `fromDate` | No | Start date in `yyyy-MM-dd` format |
| `toDate` | No | End date in `yyyy-MM-dd` format |

Examples:

```text
GET /api/mobile/attendance/history?employeeId=20&date=2026-07-06
GET /api/mobile/attendance/history?employeeId=20&fromDate=2026-07-01&toDate=2026-07-06
```

Success response:

```json
{
  "success": true,
  "message": "Attendance history fetched successfully.",
  "employeeId": 20,
  "fromDate": "2026-07-01",
  "toDate": "2026-07-06",
  "attendanceHistory": [
    {
      "attendanceId": 501,
      "attendanceDate": "2026-07-06",
      "checkInTime": "09:35:00",
      "checkOutTime": "18:10:00",
      "checkInLatitude": 19.0760000,
      "checkInLongitude": 72.8777000,
      "checkInLocationAddress": "Mumbai Office Gate 1",
      "checkOutLatitude": 19.0781000,
      "checkOutLongitude": 72.8799000,
      "checkOutLocationAddress": "Mumbai Office Gate 2",
      "inTime": "09:35",
      "outTime": "18:10",
      "totalHours": "08:35",
      "status": "PRESENT",
      "attendanceSource": "MOBILE_APP",
      "mobileAppStatus": "Y",
      "apiStatus": "N",
      "checkedIn": true,
      "checkedOut": true
    }
  ]
}
```

Attendance source flags:

| Record source | `mobileAppStatus` | `apiStatus` |
| --- | --- | --- |
| Mobile app | `Y` | `N` |
| External attendance API | `N` | `Y` |
| No attendance, leave, holiday, week off, tour, or web/manual | `N` | `N` |

The history endpoint returns one entry per requested date. It uses `LEAVE`, `COMP_OFF`, `TOUR`, `HOLIDAY`, or `WEEK_OFF` when applicable; otherwise a working date without mobile attendance is returned as `ABSENT`.

Common attendance errors:

| Code | Meaning |
| --- | --- |
| `IMAGE_REQUIRED` | Attendance image is required |
| `INVALID_IMAGE` | Image data is invalid |
| `LATITUDE_REQUIRED` | Latitude is missing |
| `LONGITUDE_REQUIRED` | Longitude is missing |
| `INVALID_LATITUDE` | Latitude is outside allowed range |
| `INVALID_LONGITUDE` | Longitude is outside allowed range |
| `INVALID_ATTENDANCE_FLAG` | Attendance flag is invalid |
| `ATTENDANCE_FLAG_REQUIRED` | Attendance flag is required |

## 10. Leave Management

These APIs expose the same leave workflows used by the employee and HOD web portal. All endpoints require a bearer access token. The supplied `employeeId` must belong to the logged-in token user.

For Swagger UI, Postman import, contract testing, or SDK generation, use the [OpenAPI 3.1 leave specification](mobile-leave-api-openapi.yaml) as the authoritative machine-readable contract.

### GET `/api/mobile/leaves/options?employeeId={employeeId}`

Returns the leave types maintained in `leave_master`, the comp-off option, and the allowed leave categories. Use the returned `code` values when submitting leave.

Success response:

```json
{
  "success": true,
  "message": "Leave application options fetched successfully.",
  "employeeId": 20,
  "leaveTypes": [
    {
      "leaveId": 1,
      "code": "CL",
      "name": "Casual Leave",
      "compOff": false
    },
    {
      "leaveId": 5,
      "code": "CO",
      "name": "Comp Off",
      "compOff": true
    }
  ],
  "leaveCategories": [
    { "code": "FULL_DAY", "name": "Full Day" },
    { "code": "FIRST_HALF", "name": "First Half" },
    { "code": "SECOND_HALF", "name": "Second Half" }
  ]
}
```

### POST `/api/mobile/leaves`

Submits a leave request for the logged-in employee.

Request:

```json
{
  "employeeId": 20,
  "leaveType": "CL",
  "leaveCategory": "FULL_DAY",
  "startDate": "2026-08-15",
  "endDate": "2026-08-16",
  "compOffWorkDate": null,
  "description": "Personal work"
}
```

Rules:

- `leaveType` must be a code returned by the options API. A returned leave name is also accepted.
- `leaveCategory` must be `FULL_DAY`, `FIRST_HALF`, or `SECOND_HALF`.
- `endDate` cannot be earlier than `startDate`.
- `description` and HOD remarks are limited to 500 characters.
- For a comp-off request, `compOffWorkDate` is required, cannot be in the future, must be earlier than `startDate`, must have qualifying attendance or an approved tour, and cannot already be used by a pending/approved comp-off request.

Success: `201 Created`, with a `Location` header such as `/api/mobile/leaves/901`.

```json
{
  "success": true,
  "message": "Leave application submitted successfully.",
  "leaveApplication": {
    "leaveId": 901,
    "employeeId": 20,
    "leaveType": "CL",
    "leaveCategory": "Full Day",
    "startDate": "2026-08-15",
    "endDate": "2026-08-16",
    "compOffWorkDate": null,
    "description": "Personal work",
    "applicationDate": "2026-08-11T14:30:00",
    "status": "PENDING",
    "hodRemarks": null,
    "managerRemarks": null,
    "cancellable": true
  }
}
```

### GET `/api/mobile/leaves?employeeId={employeeId}`

Returns all applications for the employee, newest first. Each item uses the `leaveApplication` fields shown above. Possible portal statuses include `PENDING`, `APPROVED`, `REJECTED`, and `CANCELLED`. Only a `PENDING` item has `cancellable: true`.

```json
{
  "success": true,
  "message": "Leave applications fetched successfully.",
  "employeeId": 20,
  "leaveApplications": []
}
```

### GET `/api/mobile/leaves/comp-off/validate`

Query parameters:

| Parameter | Required | Description |
| --- | --- | --- |
| `employeeId` | Yes | Logged-in employee ID |
| `workedDate` | Yes | Date in `yyyy-MM-dd` format |

Example:

```text
GET /api/mobile/leaves/comp-off/validate?employeeId=20&workedDate=2026-08-09
```

```json
{
  "success": true,
  "message": "Worked date verified.",
  "employeeId": 20,
  "workedDate": "2026-08-09",
  "valid": true
}
```

### POST `/api/mobile/leaves/{leaveId}/cancel`

Cancels the logged-in employee's own pending leave. Approved, rejected, cancelled, missing, or another employee's application cannot be cancelled.

Request:

```json
{
  "employeeId": 20
}
```

The success response uses the standard `leaveApplication` object with `status: "CANCELLED"` and `cancellable: false`.

### GET `/api/mobile/leaves/approvals`

HOD-only endpoint. It uses the same effective reporting-manager/cell mapping as the web portal and returns both pending and processed leave lists.

Query parameters:

| Parameter | Required | Description |
| --- | --- | --- |
| `employeeId` | Yes | Logged-in HOD employee ID |
| `query` | No | Employee-name or designation search |

```json
{
  "success": true,
  "message": "Leave approvals fetched successfully.",
  "employeeId": 50,
  "query": null,
  "pendingLeaves": [
    {
      "leaveId": 901,
      "applicantEmployeeId": 20,
      "employeeCode": "EMP020",
      "employeeName": "Example Employee",
      "designation": "Assistant",
      "leaveType": "CL",
      "leaveCategory": "Full Day",
      "startDate": "2026-08-15",
      "endDate": "2026-08-16",
      "compOffWorkDate": null,
      "description": "Personal work",
      "applicationDate": "2026-08-11T14:30:00",
      "status": "PENDING",
      "hodRemarks": null
    }
  ],
  "processedLeaves": []
}
```

### POST `/api/mobile/leaves/approvals/{leaveId}/approve`

HOD-only endpoint. The leave must be pending and its employee must be inside the logged-in HOD's effective reporting authority.

```json
{
  "employeeId": 50,
  "remarks": "Approved"
}
```

### POST `/api/mobile/leaves/approvals/{leaveId}/reject`

Uses the same authorization rules as approval. A non-blank `remarks` value is required.

```json
{
  "employeeId": 50,
  "remarks": "Insufficient leave notice"
}
```

Both decision endpoints return the standard `leaveApplication` response with status `APPROVED` or `REJECTED`. Processing uses a database write lock so concurrent decisions cannot process the same pending request twice.

Common leave errors:

| HTTP | Code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_LEAVE_TYPE` | Leave type was not returned by the options API |
| 400 | `INVALID_LEAVE_CATEGORY` | Leave category is invalid |
| 400 | `INVALID_LEAVE_APPLICATION` | Dates or comp-off business rules failed |
| 400 | `REJECTION_REMARKS_REQUIRED` | Rejection remarks are blank |
| 403 | `EMPLOYEE_MISMATCH` | Requested employee does not match the token user |
| 403 | `HOD_ACCESS_REQUIRED` | Logged-in user does not have HOD access |
| 403 | `LEAVE_CANCELLATION_FORBIDDEN` | Employee tried to cancel another employee's leave |
| 403 | `LEAVE_APPROVAL_FORBIDDEN` | Leave is outside the HOD's reporting authority |
| 404 | `LEAVE_NOT_FOUND` | Leave application does not exist |
| 409 | `LEAVE_NOT_CANCELLABLE` | Leave is no longer pending |
| 409 | `LEAVE_ALREADY_PROCESSED` | Approval/rejection was already completed |

## 11. Mobile App Implementation Notes

Android and iOS app should follow these rules:

- Use HTTPS only.
- Store access and refresh tokens securely in platform secure storage.
- Always send `Authorization: Bearer <accessToken>` after login or refresh.
- Refresh proactively 2 to 5 minutes before `expiresAt`; if a protected API still returns `401 INVALID_TOKEN`, call refresh once and retry the original request once.
- Use `empId` from login response as `employeeId`.
- After contact update, replace the old token with the new `accessToken` returned by the API.
- Do not hardcode location radius in the app. Use the radius returned by mapped locations API.
- For image APIs, prefer multipart upload when possible. Use JSON Base64 only when multipart is not suitable.
- Do not store passwords in the app.
- For forgot password without login, use a separate OTP/token API. Do not use the authenticated reset endpoint.

## 12. Suggested Future APIs

These APIs are not part of the current implementation but are recommended for a complete mobile app flow.

### Device Registration

Recommended endpoint:

```text
POST /api/mobile/devices
```

Purpose:

- Store Android/iOS device details.
- Store FCM/APNS push token.
- Track app version and device login.

Recommended table:

```text
mobile_user_device
```

Recommended columns:

```text
id
user_id
employee_id
device_id
platform
push_token
app_version
os_version
is_active
last_login_at
created_at
updated_at
```

### Forgot Password With OTP

Implemented endpoints:

```text
POST /api/mobile/auth/password-reset/request-otp
POST /api/mobile/auth/password-reset/verify-otp
POST /api/mobile/auth/password-reset/reset
```

Implemented table:

```text
password_reset_request
```

Recommended columns:

```text
id
user_id
otp_hash
expires_at
attempt_count
is_verified
consumed_at
created_at
updated_at
```
