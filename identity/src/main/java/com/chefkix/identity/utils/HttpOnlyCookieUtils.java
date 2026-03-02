package com.chefkix.identity.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;

@RequiredArgsConstructor
public class HttpOnlyCookieUtils {
  /**
   * Creates and adds an HttpOnly cookie to the response with proper SameSite attribute.
   * Uses ResponseCookie for better control over cookie attributes.
   * 
   * CRITICAL: Sets domain to "localhost" (without port) so cookies are shared
   * between frontend (localhost:3000) and backend (localhost:8888).
   * Without explicit domain, cookies are port-specific and cannot be shared.
   */
  public static void addHttpOnlyCookie(
      HttpServletResponse response, String name, String value, int maxAgeInSeconds) {
    // Use ResponseCookie for proper SameSite support
    ResponseCookie cookie = ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(false) // Set to true in production with HTTPS
        .domain("localhost") // CRITICAL: Share cookie across all localhost ports
        .path("/")
        .maxAge(maxAgeInSeconds)
        .sameSite("Lax") // Lax is safe for same-site navigation, prevents CSRF
        .build();
    
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
    // Use ResponseCookie for proper SameSite support
    ResponseCookie cookie = ResponseCookie.from(name, "")
        .httpOnly(true)
        .secure(false) // Match the addHttpOnlyCookie settings
        .domain("localhost") // CRITICAL: Must match the domain used when setting cookie
        .path("/")
        .maxAge(0) // Immediately expire = delete
        .sameSite("Lax")
        .build();
    
    response.addHeader("Set-Cookie", cookie.toString());
  }
}
