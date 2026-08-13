# HTTP Security Headers Audit

Audit scope: the complete Maven reactor, Spring Security configuration, servlet
filters, application profile properties, Thymeleaf/JavaScript/CSS resources, and
repository-managed deployment/proxy files.

## Header audit

| Header | Status before change | Configured in | Change |
|---|---|---|---|
| `X-Content-Type-Options` | Present as `nosniff` | Spring Security and outer response-header filter | Enforced for normal, static, redirect, error, and early-rejection responses |
| `X-Frame-Options` | Present as `SAMEORIGIN` | `SecurityConfig` | Default changed to `DENY`; only known same-origin invoice frame responses retain `SAMEORIGIN` |
| `Referrer-Policy` | Missing | `SecurityResponseHeadersFilter` and Spring Security | Added as `strict-origin-when-cross-origin` |
| `Permissions-Policy` | Missing | `SecurityResponseHeadersFilter` and Spring Security | Added with camera, microphone, geolocation, payment, USB, accelerometer, gyroscope, and magnetometer disabled |
| `Content-Security-Policy` | Missing | `SecurityResponseHeadersFilter` and Spring Security | Added with a compatibility-reviewed enforcing policy |
| `Strict-Transport-Security` | Present on secure requests | `SecurityConfig` | Reused unchanged: one year, include subdomains, no preload |
| `X-XSS-Protection` | Present as `0` | Spring Security and outer response-header filter | Explicitly emitted as `0`; legacy `1; mode=block` was not enabled because browser XSS auditors can create vulnerabilities |
| Cache headers | Present; cache writer registered redundantly | Spring Security default and `SecurityConfig` | Not changed because it is outside the reported vulnerability |

## Existing implementation inventory

- One `SecurityFilterChain` exists in `maharecruitment-web`; there is no
  `WebSecurityConfigurerAdapter` or second security chain.
- Existing filters include host validation, cookie attributes, credential
  transport enforcement/decryption, mobile bearer authentication, agency account
  status, request logging, and duplicate-context redirect handling.
- `SecurityResponseHeadersFilter` now runs at the outermost servlet-filter order
  for REQUEST, FORWARD, ERROR, INCLUDE, and ASYNC dispatches. It sets the policy
  before the chain and restores it after response resets, covering responses
  completed before Spring Security's `HeaderWriterFilter`.
- No Nginx, Apache HTTPD, ingress, API gateway, load-balancer, Docker, Kubernetes,
  Helm, or equivalent edge header configuration exists in this repository.
  Headers added outside the repository must therefore be checked in the deployed
  environment.
- `WEB-INF/undertow-handlers.conf` only configures `SameSite=Lax`; it does not add
  these response headers.

## CSP compatibility decisions

The application currently contains extensive inline scripts, event handlers,
inline styles, and style attributes. It also loads Select2/jQuery, Flatpickr, and
Leaflet from jsDelivr, code.jquery.com, and unpkg.com. The location form connects
to Nominatim and loads OpenStreetMap tile images. Image previews require `data:`
and `blob:`. Consequently, the enforcing policy temporarily permits
`'unsafe-inline'` for scripts and styles and allowlists only those observed
origins. It does not permit `'unsafe-eval'`.

Four invoice flows intentionally use same-origin iframes. Default responses use
`X-Frame-Options: DENY` and `frame-ancestors 'none'`; only the invoice responses
that are actually embedded use `SAMEORIGIN` and `frame-ancestors 'self'`.

Longer term, self-host third-party assets and replace inline scripts, event
handlers, and styles with external resources or per-response nonces. The temporary
CDN and `'unsafe-inline'` allowances can then be removed.

## HSTS and reverse-proxy finding

HSTS is correctly restricted to requests that the servlet container identifies
as secure, so local HTTP does not receive HSTS. UAT and production use
`server.forward-headers-strategy=framework`, require HTTPS, and trust forwarding
headers by default because those profiles depend on trusted TLS termination.
The edge proxy must strip client-supplied forwarding headers before setting its
authoritative `Forwarded` or `X-Forwarded-*` values.

Before deployment, choose and test one of these models:

1. Terminate TLS in the application container so `request.isSecure()` is true.
2. At a trusted reverse proxy, strip client-supplied forwarding headers, set the
   authoritative forwarded protocol, and configure Spring's forwarded-header
   strategy through secured deployment configuration.

The bare context URL (for example `/maharecruitment` without the trailing slash)
can be redirected by the servlet container before application filters execute.
Scan `/maharecruitment/` or a concrete application route such as
`/maharecruitment/login`. Configure the ingress itself to attach the same headers
to any redirect or error that it generates outside the application context.
`docs/nginx-security-headers.conf` provides a reference HTTPS virtual-host
configuration, including the context-root redirect and invoice framing exception.

## Verification

```bash
curl -I http://localhost:8080/
curl -k -I https://localhost:8443/
```

The HTTP response must omit HSTS. The HTTPS response must include HSTS plus
`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`,
`Permissions-Policy`, and `Content-Security-Policy`.

Also inspect document, REST error, static asset, redirect, and invalid-request
responses in browser DevTools and repeat active checks with OWASP ZAP or Burp
Suite. Use SecurityHeaders.com only against a deliberately internet-accessible
test or production URL.
