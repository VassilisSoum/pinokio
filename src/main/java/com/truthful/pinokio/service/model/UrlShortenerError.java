package com.truthful.pinokio.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UrlShortenerError {

  HASH_ALREADY_IN_USE("Hash already in use"),
  INTERNAL_ERROR("Internal error");

  private final String message;

}
