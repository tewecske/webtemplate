package webtemplate.backendzio

import zio.http.Headers
import webtemplate.shared.config.AuthConfig

/** Baseline security headers for JSON API responses. See CWE-693 / OWASP A05:2021. The API
  * never returns HTML, so a maximally strict CSP is safe here. HSTS is only sent when
  * `secureCookie` is true (prod), since it would be meaningless advice over plain dev HTTP.
  */
object SecurityHeaders {
  def apply(config: AuthConfig): Headers = {
    val base = Headers(
      ("X-Content-Type-Options", "nosniff"),
      ("Referrer-Policy", "no-referrer"),
      ("X-Frame-Options", "DENY"),
      ("Content-Security-Policy", "default-src 'none'"),
      ("Permissions-Policy", "geolocation=(), camera=(), microphone=(), interest-cohort=()")
    )
    if (config.secureCookie)
      base ++ Headers(("Strict-Transport-Security", "max-age=15552000; includeSubDomains"))
    else base
  }
}
