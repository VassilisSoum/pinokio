package com.truthful.pinokio.repository;

import com.truthful.pinokio.service.model.ApplicationConfigurationProperties;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

  private final RedisTemplate<String, String> redisTemplate;
  private final ApplicationConfigurationProperties applicationConfigurationProperties;

  @PostConstruct
  void init() {
    redisTemplate.execute(
        (RedisCallback<Object>)
            connection -> {
              connection.execute(
                  "CF.RESERVE",
                  applicationConfigurationProperties.getRedisFilterName().getBytes(),
                  applicationConfigurationProperties.getRedisFilterCapacity().toString().getBytes(),
                  applicationConfigurationProperties.getRedisFilterBuckets().toString().getBytes(),
                  applicationConfigurationProperties
                      .getRedisFilterMaxIterations()
                      .toString()
                      .getBytes());
              return null;
            });
  }

  public Try<Boolean> isHashAlreadyInUse(String hash) {
    return Try.ofCallable(
        () ->
            redisTemplate.execute(
                (RedisCallback<Boolean>)
                    connection -> {
                      Object response =
                          connection.execute(
                              "CF.EXISTS",
                              applicationConfigurationProperties.getRedisFilterName().getBytes(),
                              hash.getBytes());
                      return response != null && response.equals(1L);
                    }));
  }

  public Try<Void> saveHash(String hash) {
    return Try.ofCallable(
        () ->
            redisTemplate.execute(
                (RedisCallback<Void>)
                    connection -> {
                      connection.execute(
                          "CF.ADD",
                          applicationConfigurationProperties.getRedisFilterName().getBytes(),
                          hash.getBytes());
                      return null;
                    }));
  }

  public Try<Void> deleteHash(String hash) {
    return Try.ofCallable(
        () ->
            redisTemplate.execute(
                (RedisCallback<Void>)
                    connection -> {
                      connection.execute(
                          "CF.DEL",
                          applicationConfigurationProperties.getRedisFilterName().getBytes(),
                          hash.getBytes());
                      return null;
                    }));
  }
}
