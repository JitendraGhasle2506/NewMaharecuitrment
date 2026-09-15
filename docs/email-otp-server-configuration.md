# Email OTP server configuration

Email OTP uses the common Spring Mail configuration packaged in the WAR. Local, UAT and production inherit the same AWS SES SMTP host, port, credentials, sender and TLS settings from `application.properties`; each profile keeps email enabled by default.

The AWS SES SMTP username and password are deliberately packaged as fixed values for this deployment. The same credentials are used by local, UAT and production; runtime `SMTP_USERNAME` and `SMTP_PASSWORD` variables do not override them.

## Required server environment

Configure these variables for the operating-system account that runs the application server, then restart the application server:

```text
SPRING_PROFILES_ACTIVE=<uat or prod>
EMAIL_ENABLED=true
SMTP_HOST=email-smtp.ap-south-1.amazonaws.com
SMTP_PORT=587
SMTP_FROM_EMAIL=<verified AWS SES sender address>
SMTP_AUTH=true
SMTP_STARTTLS_ENABLED=true
SMTP_STARTTLS_REQUIRED=true
SMTP_SSL_ENABLED=false
SMTP_TEST_CONNECTION=true
```

`SMTP_TEST_CONNECTION=true` makes deployment fail at startup when the SMTP server cannot be reached or authenticated. This is recommended while diagnosing a deployment; it can be set to `false` afterward if email availability should not prevent application startup.

The variables above configure the profile, connection and sender. SMTP credentials come from the packaged application configuration.

## Connectivity check

Run the appropriate check on the deployed server itself, not on a developer machine.

Linux:

```bash
openssl s_client -crlf -quiet -starttls smtp -connect email-smtp.ap-south-1.amazonaws.com:587
```

Windows Server:

```powershell
Test-NetConnection email-smtp.ap-south-1.amazonaws.com -Port 2587
```

If port 2587 is blocked but port 465 is permitted, use the TLS-wrapper settings below:

```text
SMTP_PORT=465
SMTP_STARTTLS_ENABLED=false
SMTP_STARTTLS_REQUIRED=false
SMTP_SSL_ENABLED=true
```

Do not use the port 465 settings with STARTTLS enabled. If neither port connects, allow outbound TCP 2587 or 465 in the host firewall, network firewall, security group, and proxy policy.

## Reading the failure

Search the production log for `Failed to send email verification OTP`. The entry now reports a masked recipient, SMTP host and port, and the deepest exception type/message without logging the OTP or SMTP password.

Common failure meanings:

- `ConnectException`, `SocketTimeoutException`, or `UnknownHostException`: DNS, route, firewall, security group, or proxy problem.
- `AuthenticationFailedException` or SMTP `535`: incorrect/revoked AWS SES SMTP credentials or an account authentication restriction.
- SMTP `550`/`554`: sender alias, recipient, account policy, quota, or reputation rejection.
- `SSLHandshakeException`: server trust store, TLS interception, or an incorrect STARTTLS/TLS-wrapper combination.

Because the fixed credentials can be extracted from a WAR, restrict access to the artifact and rotate the AWS SES SMTP credentials whenever the WAR is shared outside the authorized deployment path.
