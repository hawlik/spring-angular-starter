package com.example.app.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class DeviceFingerprintTest {

  private static final String CHROME_UA =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
          + "Chrome/147.0.0.0 Safari/537.36";

  @Nested
  @DisplayName("compute")
  class Compute {

    @Test
    @DisplayName("returns null when user agent is null")
    void nullUaReturnsNull() {
      assertThat(DeviceFingerprint.compute(null, "en-US", null, null)).isNull();
    }

    @Test
    @DisplayName("returns null when user agent is blank")
    void blankUaReturnsNull() {
      assertThat(DeviceFingerprint.compute("   ", "en-US", null, null)).isNull();
    }

    @Test
    @DisplayName("returns 64-char hex digest for a non-blank user agent")
    void returnsHexDigest() {
      String result = DeviceFingerprint.compute(CHROME_UA, "en-US", null, "\"Linux\"");

      assertThat(result).isNotNull().hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("is stable across calls with identical inputs")
    void stableAcrossCalls() {
      String a = DeviceFingerprint.compute(CHROME_UA, "en-US", "\"Chromium\";v=\"147\"", "\"Linux\"");
      String b = DeviceFingerprint.compute(CHROME_UA, "en-US", "\"Chromium\";v=\"147\"", "\"Linux\"");

      assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("distinguishes Brave from Chrome via Sec-CH-UA when user agents are identical")
    void braveAndChromeDifferWhenSecChUaDiffers() {
      String chromeSecChUa =
          "\"Google Chrome\";v=\"147\", \"Chromium\";v=\"147\", \"Not?A_Brand\";v=\"24\"";
      String braveSecChUa =
          "\"Chromium\";v=\"147\", \"Not?A_Brand\";v=\"24\", \"Brave\";v=\"1.85\"";

      String chromeFp = DeviceFingerprint.compute(CHROME_UA, "en-US", chromeSecChUa, "\"Linux\"");
      String braveFp = DeviceFingerprint.compute(CHROME_UA, "en-US", braveSecChUa, "\"Linux\"");

      assertThat(chromeFp).isNotEqualTo(braveFp);
    }

    @Test
    @DisplayName("treats null and empty Accept-Language as equivalent")
    void nullAcceptLanguageEqualsEmpty() {
      String a = DeviceFingerprint.compute(CHROME_UA, null, null, null);
      String b = DeviceFingerprint.compute(CHROME_UA, "", null, null);

      assertThat(a).isEqualTo(b);
    }
  }

  @Nested
  @DisplayName("label")
  class Label {

    @Test
    @DisplayName("returns null for null user agent")
    void nullReturnsNull() {
      assertThat(DeviceFingerprint.label(null)).isNull();
    }

    @Test
    @DisplayName("returns null for blank user agent")
    void blankReturnsNull() {
      assertThat(DeviceFingerprint.label("   ")).isNull();
    }

    @Test
    @DisplayName("returns trimmed user agent unchanged when short enough")
    void returnsTrimmedUserAgent() {
      assertThat(DeviceFingerprint.label("  " + CHROME_UA + "  ")).isEqualTo(CHROME_UA);
    }

    @Test
    @DisplayName("truncates user agents longer than 255 characters")
    void truncatesLongUserAgent() {
      String longUa = "a".repeat(300);

      String result = DeviceFingerprint.label(longUa);

      assertThat(result).hasSize(255);
    }
  }

  @Nested
  @DisplayName("summary")
  class Summary {

    @Test
    @DisplayName("returns Brave when Sec-CH-UA contains Brave brand entry")
    void detectsBraveFromSecChUa() {
      String secChUa = "\"Chromium\";v=\"147\", \"Not?A_Brand\";v=\"24\", \"Brave\";v=\"1.85\"";

      String result = DeviceFingerprint.summary(secChUa, "\"Linux\"", CHROME_UA);

      assertThat(result).isEqualTo("Brave 1.85 on Linux");
    }

    @Test
    @DisplayName("returns Google Chrome when Sec-CH-UA lists it as the real brand")
    void detectsChromeFromSecChUa() {
      String secChUa = "\"Google Chrome\";v=\"147\", \"Chromium\";v=\"147\", \"Not?A_Brand\";v=\"24\"";

      String result = DeviceFingerprint.summary(secChUa, "\"Linux\"", CHROME_UA);

      assertThat(result).isEqualTo("Google Chrome 147 on Linux");
    }

    @Test
    @DisplayName("returns Microsoft Edge when Sec-CH-UA lists it as the real brand")
    void detectsEdgeFromSecChUa() {
      String secChUa = "\"Microsoft Edge\";v=\"147\", \"Chromium\";v=\"147\", \"Not?A_Brand\";v=\"24\"";

      String result = DeviceFingerprint.summary(secChUa, "\"Windows\"", CHROME_UA);

      assertThat(result).isEqualTo("Microsoft Edge 147 on Windows");
    }

    @Test
    @DisplayName("skips alternate GREASE spellings")
    void skipsGreaseVariations() {
      String secChUa = "\"Not.A/Brand\";v=\"99\", \"Brave\";v=\"1.85\"";

      String result = DeviceFingerprint.summary(secChUa, null, CHROME_UA);

      assertThat(result).startsWith("Brave 1.85");
    }

    @Test
    @DisplayName("falls back to user-agent parsing when Sec-CH-UA is absent (Chrome)")
    void fallsBackToUaForChrome() {
      String result = DeviceFingerprint.summary(null, null, CHROME_UA);

      assertThat(result).isEqualTo("Chrome 147 on Linux");
    }

    @Test
    @DisplayName("identifies Firefox from user-agent when Sec-CH-UA is absent")
    void detectsFirefoxFromUa() {
      String firefoxUa =
          "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0";

      String result = DeviceFingerprint.summary(null, null, firefoxUa);

      assertThat(result).isEqualTo("Firefox 121 on Linux");
    }

    @Test
    @DisplayName("identifies Safari only when Version/ is present and Chrome is not")
    void detectsRealSafari() {
      String safariUa =
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
              + "(KHTML, like Gecko) Version/17.4 Safari/605.1.15";

      String result = DeviceFingerprint.summary(null, null, safariUa);

      assertThat(result).isEqualTo("Safari 17 on macOS");
    }

    @Test
    @DisplayName("strips quotes from Sec-CH-UA-Platform header value")
    void stripsPlatformQuotes() {
      String result = DeviceFingerprint.summary(
          "\"Brave\";v=\"1.85\"", "\"macOS\"", CHROME_UA);

      assertThat(result).endsWith("on macOS");
    }

    @Test
    @DisplayName("returns null when no browser nor OS can be derived")
    void returnsNullWhenNothingMatches() {
      String result = DeviceFingerprint.summary(null, null, "curl/8.0.0");

      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("from")
  class From {

    @Test
    @DisplayName("returns DeviceInfo with all three fields populated for a typical browser request")
    void returnsFullDeviceInfo() {
      DeviceInfo info = DeviceFingerprint.from(
          CHROME_UA,
          "en-US",
          "\"Chromium\";v=\"147\", \"Brave\";v=\"1.85\"",
          "\"Linux\"");

      assertThat(info.fingerprint()).hasSize(64);
      assertThat(info.label()).isEqualTo(CHROME_UA);
      assertThat(info.summary()).isEqualTo("Brave 1.85 on Linux");
    }

    @Test
    @DisplayName("returns null fields when no user agent is provided")
    void returnsNullFieldsForMissingUa() {
      DeviceInfo info = DeviceFingerprint.from(null, null, null, null);

      assertThat(info.fingerprint()).isNull();
      assertThat(info.label()).isNull();
      assertThat(info.summary()).isNull();
    }

    @Test
    @DisplayName("reads the four relevant headers from HttpServletRequest")
    void extractsHeadersFromRequest() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("User-Agent", CHROME_UA);
      request.addHeader("Accept-Language", "en-US");
      request.addHeader("Sec-CH-UA", "\"Chromium\";v=\"147\", \"Brave\";v=\"1.85\"");
      request.addHeader("Sec-CH-UA-Platform", "\"Linux\"");

      DeviceInfo info = DeviceFingerprint.from(request);

      assertThat(info.fingerprint()).hasSize(64);
      assertThat(info.label()).isEqualTo(CHROME_UA);
      assertThat(info.summary()).isEqualTo("Brave 1.85 on Linux");
    }

    @Test
    @DisplayName("returns empty DeviceInfo when request has no User-Agent header")
    void returnsEmptyForRequestWithoutUserAgent() {
      MockHttpServletRequest request = new MockHttpServletRequest();

      DeviceInfo info = DeviceFingerprint.from(request);

      assertThat(info.fingerprint()).isNull();
      assertThat(info.label()).isNull();
      assertThat(info.summary()).isNull();
    }
  }
}
