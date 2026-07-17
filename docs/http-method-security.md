# HTTP method security policy

The application accepts `GET`, `HEAD`, `POST`, `PUT`, `DELETE`, and `PATCH`. `TRACE`,
`TRACK`, `DEBUG`, `CONNECT`, unknown extension methods, and (by default) `OPTIONS`
receive HTTP 405 with a JSON error before authentication or controller dispatch.
`HEAD` remains enabled because HTTP defines it as the metadata-only counterpart of
`GET`, and Spring MVC supports it automatically for GET mappings.

`HttpMethodPolicyFilter` is placed first in the Spring Security chain. The separate
servlet registration is disabled to avoid executing it twice. Embedded Tomcat is
also customized with `allowTrace=false`; this is defence in depth and does not
replace the application policy filter.

## CORS

CORS is not configured in this application, so `OPTIONS` is disabled. If a real
cross-origin client is introduced, define a least-privilege Spring
`CorsConfiguration` (specific origins, headers, and only the required methods),
enable `http.cors(...)`, and set:

```properties
app.security.http-methods.allow-options=true
```

Never enable the property without the matching restricted CORS configuration.

## Standalone Apache Tomcat

For WAR deployment, configure every HTTP/HTTPS connector in `conf/server.xml`:

```xml
<Connector port="8080" protocol="org.apache.coyote.http11.Http11NioProtocol"
           allowTrace="false" />
```

Restart Tomcat after the change. The embedded-server customizer does not configure
an externally managed Tomcat instance.

## Nginx reverse proxy

Place the `map` in the `http` context and the `if` in the applicable `server`
block, before proxying, then reload Nginx:

```nginx
map $request_method $method_not_allowed {
    default 1;
    GET 0;
    HEAD 0;
    POST 0;
    PUT 0;
    DELETE 0;
    PATCH 0;
}

# Inside server { ... }
if ($method_not_allowed) {
    return 405;
}

location / {
    proxy_pass http://maharecruitment_upstream;
}
```

If CORS is later required, add `OPTIONS 0` only with explicit preflight handling
and a method/origin allowlist. The application still provides
the canonical JSON 405 body; the proxy response may use its own body.

## Apache HTTP Server reverse proxy

Enable `mod_rewrite`, then place this in the virtual host and restart or gracefully
reload Apache:

```apache
TraceEnable off
RewriteEngine On
RewriteCond %{REQUEST_METHOD} !^(GET|HEAD|POST|PUT|DELETE|PATCH)$ [NC]
RewriteRule ^ - [R=405,L]
```

If CORS is required, remove `OPTIONS` only after adding a narrow CORS policy.

## Verification and controller audit

Automated tests cover all accepted application methods, all named dangerous
methods, disabled/enabled preflight behavior, and an unknown method. The controller
audit found one handler-level generic `@RequestMapping`, the `/error` dispatcher;
it is now explicitly restricted. Remaining generic mappings are class-level path
prefixes and do not independently expose handler methods.

At each deployed ingress, verify at least:

```bash
curl -i -X TRACE https://host/maharecruitment/api/example
curl -i -X TRACK https://host/maharecruitment/api/example
curl -i -X CONNECT https://host/maharecruitment/api/example
curl -i -X OPTIONS https://host/maharecruitment/api/example
```

Each request must return 405. Re-run these checks whenever CORS, a reverse proxy,
or a servlet connector is changed.
