package com.example.app.auth.security;

public record DeviceInfo(String fingerprint, String label, String summary) {

  public static DeviceInfo none() {
    return new DeviceInfo(null, null, null);
  }
}
