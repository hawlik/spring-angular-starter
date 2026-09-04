package com.example.app.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeviceFingerprint {

  private static final Pattern BRAND_ENTRY =
      Pattern.compile("\"([^\"]+)\"\\s*;\\s*v=\"([^\"]+)\"");
  private static final int MAX_LABEL = 255;
  private static final int MAX_SUMMARY = 120;

  private DeviceFingerprint() {}

  public static DeviceInfo from(HttpServletRequest request) {
    return from(
        request.getHeader("User-Agent"),
        request.getHeader("Accept-Language"),
        request.getHeader("Sec-CH-UA"),
        request.getHeader("Sec-CH-UA-Platform"));
  }

  public static DeviceInfo from(
      String userAgent,
      String acceptLanguage,
      String secChUa,
      String secChUaPlatform
  ) {
    return new DeviceInfo(
        compute(userAgent, acceptLanguage, secChUa, secChUaPlatform),
        label(userAgent),
        summary(secChUa, secChUaPlatform, userAgent));
  }

  static String compute(
      String userAgent,
      String acceptLanguage,
      String secChUa,
      String secChUaPlatform
  ) {
    if (userAgent == null || userAgent.isBlank()) {
      return null;
    }
    // NUL (U+0000) cannot appear in HTTP header values (rejected by the servlet container),
    // so it is a collision-safe delimiter between header components.
    String material = String.join("\u0000",
        userAgent.trim(),
        nullSafe(acceptLanguage),
        nullSafe(secChUa),
        nullSafe(secChUaPlatform));
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(material.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  static String label(String userAgent) {
    if (userAgent == null || userAgent.isBlank()) {
      return null;
    }
    String trimmed = userAgent.trim();
    if (trimmed.length() > MAX_LABEL) {
      return trimmed.substring(0, MAX_LABEL);
    }
    return trimmed;
  }

  static String summary(String secChUa, String secChUaPlatform, String userAgent) {
    String browser = brandFromSecChUa(secChUa);
    if (browser == null) {
      browser = browserFromUserAgent(userAgent);
    }
    String os = platformFromHeader(secChUaPlatform);
    if (os == null) {
      os = osFromUserAgent(userAgent);
    }

    String combined;
    if (browser != null && os != null) {
      combined = browser + " on " + os;
    } else if (browser != null) {
      combined = browser;
    } else if (os != null) {
      combined = os;
    } else {
      return null;
    }

    if (combined.length() > MAX_SUMMARY) {
      return combined.substring(0, MAX_SUMMARY);
    }
    return combined;
  }

  private static String brandFromSecChUa(String secChUa) {
    if (secChUa == null || secChUa.isBlank()) {
      return null;
    }
    Matcher m = BRAND_ENTRY.matcher(secChUa);
    while (m.find()) {
      String brand = m.group(1);
      String version = m.group(2);
      if (isGrease(brand) || "Chromium".equalsIgnoreCase(brand)) {
        continue;
      }
      return brand + " " + version;
    }
    return null;
  }

  // GREASE entries vary across browsers and versions to deter strict whitelisting,
  // e.g. "Not?A_Brand", "Not.A/Brand", "Not_A Brand", "Not-A.Brand".
  private static boolean isGrease(String brand) {
    String lower = brand.toLowerCase();
    return lower.contains("brand") && lower.contains("not");
  }

  private static String platformFromHeader(String secChUaPlatform) {
    if (secChUaPlatform == null || secChUaPlatform.isBlank()) {
      return null;
    }
    String value = secChUaPlatform.trim();
    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
      value = value.substring(1, value.length() - 1);
    }
    if (value.isBlank()) {
      return null;
    }
    return value;
  }

  private static String browserFromUserAgent(String ua) {
    if (ua == null) {
      return null;
    }
    if (ua.contains("Edg/")) {
      return nameAndMajor("Edge", ua, "Edg/");
    }
    if (ua.contains("OPR/")) {
      return nameAndMajor("Opera", ua, "OPR/");
    }
    if (ua.contains("Firefox/")) {
      return nameAndMajor("Firefox", ua, "Firefox/");
    }
    if (ua.contains("Chrome/")) {
      return nameAndMajor("Chrome", ua, "Chrome/");
    }
    if (ua.contains("Version/") && ua.contains("Safari/")) {
      return nameAndMajor("Safari", ua, "Version/");
    }
    return null;
  }

  private static String nameAndMajor(String name, String ua, String token) {
    int idx = ua.indexOf(token);
    if (idx < 0) {
      return name;
    }
    int start = idx + token.length();
    int end = start;
    while (end < ua.length()
        && (Character.isDigit(ua.charAt(end)) || ua.charAt(end) == '.')) {
      end++;
    }
    String version = ua.substring(start, end);
    int dot = version.indexOf('.');
    if (dot > 0) {
      version = version.substring(0, dot);
    }
    if (version.isEmpty()) {
      return name;
    }
    return name + " " + version;
  }

  private static String osFromUserAgent(String ua) {
    if (ua == null) {
      return null;
    }
    if (ua.contains("Windows")) {
      return "Windows";
    }
    if (ua.contains("Android")) {
      return "Android";
    }
    if (ua.contains("iPhone") || ua.contains("iPad")) {
      return "iOS";
    }
    if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
      return "macOS";
    }
    if (ua.contains("Linux") || ua.contains("X11")) {
      return "Linux";
    }
    return null;
  }

  private static String nullSafe(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }
}
