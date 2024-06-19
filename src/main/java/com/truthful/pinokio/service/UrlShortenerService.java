package com.truthful.pinokio.service;


import io.vavr.control.Try;
import java.util.Optional;

public interface UrlShortenerService {
  Try<String> shortenUrl(String url);
  Try<Optional<String>> getOriginalUrl(String shortUrl);
  Try<Void> deleteUrl(String shortUrl);
}
