package com.chefkix.identity.utils;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtils {

  private static final String[] IP_HEADERS = {
    "X-Forwarded-For",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_X_FORWARDED_FOR",
    "HTTP_X_FORWARDED",
    "HTTP_X_CLUSTER_CLIENT_IP",
    "HTTP_CLIENT_IP",
    "HTTP_FORWARDED_FOR",
    "HTTP_FORWARDED",
    "X-Real-IP"
  };

  public static String getClientIpAddress(HttpServletRequest request) {
    String ipAddress = null;

    for (String header : IP_HEADERS) {
      ipAddress = request.getHeader(header);
      if (isValidIp(ipAddress)) {
        break;
      }
    }

    if (!isValidIp(ipAddress)) {
      ipAddress = request.getRemoteAddr();
    }

    if (ipAddress != null && ipAddress.contains(",")) {
      ipAddress = ipAddress.split(",")[0].trim();
    }

    if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
      return "127.0.0.1";
    }

    return ipAddress;
  }

  private static boolean isValidIp(String ip) {
    return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
  }
}
