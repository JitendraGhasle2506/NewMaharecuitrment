# Email OTP server configuration

Email OTP uses Spring Mail. The SMTP host, port, sender, and TLS settings come from deployment configuration. The SMTP username and password are loaded from the `smtp_configuration` database table for every send and are not packaged in the WAR.

Migration `V136__smtp_configuration` creates the table. Configure its singleton row after deploying the application:

```sql
insert into smtp_configuration
    (configuration_id, username, password, enabled, updated_at)
values
    (1, '<SMTP_USERNAME>', '<SMTP_PASSWORD>', true, current_timestamp)
on conflict (configuration_id) do update
set username = excluded.username,
    password = excluded.password,
    enabled = excluded.enabled,
    updated_at = current_timestamp;
```

Run this SQL through the restricted database administration path. Do not commit real credentials to source control or migration files.

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

The variables above configure the profile, connection and sender. SMTP authentication credentials come only from the database row.

## Connectivity check

Run the appropriate check on the deployed server itself, not on a developer machine.

Linux:

```bash
openssl s_client -crlf -quiet -starttls smtp -connect email-smtp.ap-south-1.amazonaws.com:587
```

Windows Server:

```powershell
Test-NetConnection email-smtp.ap-south-1.amazonaws.com -Port 587
```

If port 587 is blocked but port 465 is permitted, use the TLS-wrapper settings below:

```text
SMTP_PORT=465
SMTP_STARTTLS_ENABLED=false
SMTP_STARTTLS_REQUIRED=false
SMTP_SSL_ENABLED=true
```

Do not use the port 465 settings with STARTTLS enabled. If neither port connects, allow outbound TCP 587 or 465 in the host firewall, network firewall, security group, and proxy policy.

## Reading the failure

Search the production log for `Failed to send email verification OTP`. The entry now reports a masked recipient, SMTP host and port, and the deepest exception type/message without logging the OTP or SMTP password.

Common failure meanings:

- `ConnectException`, `SocketTimeoutException`, or `UnknownHostException`: DNS, route, firewall, security group, or proxy problem.
- `AuthenticationFailedException` or SMTP `535`: incorrect/revoked AWS SES SMTP credentials or an account authentication restriction.
- SMTP `550`/`554`: sender alias, recipient, account policy, quota, or reputation rejection.
- `SSLHandshakeException`: server trust store, TLS interception, or an incorrect STARTTLS/TLS-wrapper combination.

Restrict direct read access to `smtp_configuration` to the application account and authorized database administrators. Rotate SMTP credentials immediately if they are disclosed in chat, logs, source code, or deployment artifacts.
