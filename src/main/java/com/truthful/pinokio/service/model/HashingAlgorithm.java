package com.truthful.pinokio.service.model;

import com.google.common.hash.Hashing;

public enum HashingAlgorithm {
  MURMUR32 {
    public String generateHash(String input) {
      return Hashing.murmur3_32_fixed().hashUnencodedChars(input).toString();
    }
  },
  MURMUR128 {
    public String generateHash(String input) {
      return Hashing.murmur3_128().hashUnencodedChars(input).toString();
    }
  },
  SHA256 {
    public String generateHash(String input) {
      return Hashing.sha256().hashUnencodedChars(input).toString();
    }
  };

  public abstract String generateHash(String input);
}
