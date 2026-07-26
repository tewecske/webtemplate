package webtemplate.backendcask

import webtemplate.shared.config.AuthConfig

/** Baseline security headers for JSON API responses. See CWE-693 / OWASP A05:2021. The API
  * never returns HTML, so a maximally strict CSP is safe here. HSTS is only sent when
  * `secureCookie` is true (prod), since it would be meaningless advice over plain dev HTTP.
  */
object SecurityHeaders {
  def apply(config: AuthConfig): Seq[(String, String)] = {
    val base = Seq(
      "X-Content-Type-Options" -> "nosniff",
      "Referrer-Policy" -> "no-referrer",
      "X-Frame-Options" -> "DENY",
      "Content-Security-Policy" -> "default-src 'none'",
      "Permissions-Policy" -> "geolocation=(), camera=(), microphone=(), interest-cohort=()"
    )
    if (config.secureCookie) base :+ ("Strict-Transport-Security" -> "max-age=15552000; includeSubDomains") else base
  }
}
