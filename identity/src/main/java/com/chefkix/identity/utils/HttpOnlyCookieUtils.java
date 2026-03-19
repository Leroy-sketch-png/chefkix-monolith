package com.chefkix.identity.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

public class HttpOnlyCookieUtils {

  private static String cookieDomain = "localhost";
  private static boolean secureCookies = false;

  /**
   * Configure cookie domain and secure flag for deployment environment.
   * Call from application startup (e.g., @PostConstruct in a config bean).
   */
  public static void configure(String domain, boolean secure) {
    cookieDomain = domain;
    secureCookies = secure;
  }

  /**
   * Creates and adds an HttpOnly cookie to the response with proper SameSite attribute.
   * Uses ResponseCookie for better control over cookie attributes.
   * 
   * Domain is configurable via configure() for production deployment.
   * Without explicit domain, cookies are port-specific and cannot be shared.
   */
  public static void addHttpOnlyCookie(
      HttpServletResponse response, String name, String value, int maxAgeInSeconds) {
    ResponseCookie cookie = buildCookie(name, value, maxAgeInSeconds);
    
    response.addHeader("Set-Cookie", cookie.toString());
  }

  /** Get cookie value from request */
  public static String getCookieValue(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equals(name)) {
        return cookie.getValue();
      }
    }
    return null;
  }

  /** Delete cookie by setting maxAge = 0 */
  public static void deleteHttpOnlyCookie(HttpServletResponse response, String name) {
    ResponseCookie cookie = buildCookie(name, "", 0);

    response.addHeader("Set-Cookie", cookie.toString());
  }

  private static ResponseCookie buildCookie(String name, String value, int maxAgeInSeconds) {
    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secureCookies)
        .path("/")
        .maxAge(maxAgeInSeconds)
        .sameSite("Strict");

    // Browsers often reject `Domain=localhost`; host-only cookies are correct for local dev.
    if (StringUtils.hasText(cookieDomain) && !"localhost".equalsIgnoreCase(cookieDomain.trim())) {
      builder.domain(cookieDomain.trim());
    }

    return builder.build();
  }
}
