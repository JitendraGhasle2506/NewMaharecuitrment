# HTTPS Transport Security

The application is configured to require HTTPS for every route and to reject credential submissions that arrive over an insecure channel.

Deployment requirements:

- Terminate TLS at Tomcat or at the reverse proxy/load balancer in front of the application.
- Redirect public HTTP traffic to HTTPS before it reaches the application.
- Forward the original protocol with `X-Forwarded-Proto: https` or RFC 7239 `Forwarded: proto=https`.
- Keep `server.forward-headers-strategy=framework` enabled so Spring resolves `request.isSecure()` correctly behind the proxy.
- Strip client-supplied forwarding headers at the edge proxy before setting trusted values.
- Ensure session cookies retain `Secure`, `HttpOnly`, and `SameSite=Lax` attributes.
- Do not enable HSTS preload until the production domain and every subdomain are confirmed to be HTTPS-only.
- Default, UAT, and production profiles do not exempt localhost HTTP for credential submission.
- The `local` profile may be used for isolated developer troubleshooting only. Do not use the `local` profile for security testing or any shared environment.
