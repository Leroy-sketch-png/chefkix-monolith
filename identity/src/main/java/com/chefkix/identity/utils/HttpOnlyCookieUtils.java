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
   */
  public static void configure(String domain, boolean secure) {
    cookieDomain = domain;
    secureCookies = secure;
  }

  /**
   * 
   */
  public static void addHttpOnlyCookie(
      HttpServletResponse response, String name, String value, int maxAgeInSeconds) {
    ResponseCookie cookie = buildCookie(name, value, maxAgeInSeconds);
    
    response.addHeader("Set-Cookie", cookie.toString());
  }

  public static String getCookieValue(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equals(name)) {
        return cookie.getValue();
      }
    }
    return null;
  }

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
        .sameSite("Lax");

    if (StringUtils.hasText(cookieDomain) && !"localhost".equalsIgnoreCase(cookieDomain.trim())) {
      builder.domain(cookieDomain.trim());
    }

    return builder.build();
  }
}
