package com.truthful.pinokio.integrationtest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soumakis.control.Either;
import com.soumakis.control.Left;
import com.soumakis.control.Right;
import com.truthful.pinokio.repository.DynamoDBRepository;
import com.truthful.pinokio.service.model.UrlShortenerError;
import com.truthful.pinokio.service.model.HashingAlgorithm;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@ActiveProfiles("test")
public class DynamoDBRepositoryIT {

  @Autowired
  private DynamoDBRepository dynamoDBRepository;

  private static final LocalStackContainer localStackContainer;

  static {
    localStackContainer = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest"))
        .withCopyFileToContainer(MountableFile.forClasspathResource("init-aws.sh", 744),
            "/etc/localstack/init/ready.d/init-dynamodb.sh")
        .withServices(Service.DYNAMODB)
        .waitingFor(Wait.forLogMessage(".*Executed init-dynamodb.sh.*", 1));
    localStackContainer.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("application.dynamodb.access-key-id", localStackContainer::getAccessKey);
    registry.add("application.dynamodb.secret-access-key", localStackContainer::getSecretKey);
    registry.add("application.dynamodb.region", localStackContainer::getRegion);
    registry.add("application.dynamodb.endpoint-url", localStackContainer::getEndpoint);
  }

  @Test
  void testSaveAndLoad() {
    String originalUrl = "https://www.catnipcoder.com";
    String hash = HashingAlgorithm.MURMUR32.generateHash(originalUrl + UUID.randomUUID());
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime expiresAt = createdAt.plusDays(1);
    Either<UrlShortenerError, String> saved = dynamoDBRepository.save(hash, originalUrl, createdAt,
        expiresAt);

    assertThat(saved, is(new Right<>(hash)));

    Either<UrlShortenerError, String> maybeOriginalUrl = dynamoDBRepository.getOriginalUrl(hash);

    assertThat(maybeOriginalUrl, is(new Right<>(originalUrl)));
  }

  @Test
  void testDelete() {
    String originalUrl = "https://www.catnipcoder.com";
    String hash = HashingAlgorithm.MURMUR32.generateHash(originalUrl + UUID.randomUUID());
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime expiresAt = createdAt.plusDays(1);
    Either<UrlShortenerError, String> saved = dynamoDBRepository.save(hash, originalUrl, createdAt,
        expiresAt);

    assertThat(saved, is(new Right<>(hash)));

    Either<UrlShortenerError, String> maybeOriginalUrl = dynamoDBRepository.getOriginalUrl(hash);

    assertThat(maybeOriginalUrl, is(new Right<>(originalUrl)));

    assertThat(dynamoDBRepository.delete(hash).isSuccess(), is(true));

    Either<UrlShortenerError, String> maybeDeleted = dynamoDBRepository.getOriginalUrl(hash);

    assertThat(maybeDeleted, is(new Left<>(UrlShortenerError.HASH_NOT_FOUND)));
  }

}
