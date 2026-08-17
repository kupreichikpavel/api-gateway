package by.innowise.apigateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringJUnitConfig(
    classes = {
        SecurityConfig.class,
        SecurityConfigTest.TestConfig.class
    }
)
class SecurityConfigTest {

  @Autowired
  private ApplicationContext applicationContext;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient
        .bindToApplicationContext(applicationContext)
        .apply(springSecurity())
        .configureClient()
        .build();
  }

  @Test
  void loginShouldBePublic() {
    assertPublicPost("/api/v1/auth/login");
  }

  @Test
  void registerShouldBePublic() {
    assertPublicPost("/api/v1/auth/register");
  }

  @Test
  void refreshShouldBePublic() {
    assertPublicPost("/api/v1/auth/refresh");
  }

  @Test
  void protectedEndpointShouldReturnUnauthorizedWithoutJwt() {
    webTestClient
        .get()
        .uri("/api/v1/orders")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void protectedEndpointShouldBeAccessibleWithJwt() {
    webTestClient
        .mutateWith(mockJwt())
        .get()
        .uri("/api/v1/orders")
        .exchange()
        .expectStatus()
        .isOk();
  }

  private void assertPublicPost(String path) {
    webTestClient
        .post()
        .uri(path)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Configuration
  @EnableWebFlux
  static class TestConfig {

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
      return token ->
          Mono.error(
              new IllegalArgumentException(
                  "JWT decoder should not be called in this test"
              )
          );
    }

    @Bean
    RouterFunction<ServerResponse> testRoutes() {
      return RouterFunctions.route()
          .POST(
              "/api/v1/auth/login",
              request -> ServerResponse.ok().build()
          )
          .POST(
              "/api/v1/auth/register",
              request -> ServerResponse.ok().build()
          )
          .POST(
              "/api/v1/auth/refresh",
              request -> ServerResponse.ok().build()
          )
          .GET(
              "/api/v1/orders",
              request -> ServerResponse.ok().build()
          )
          .build();
    }
  }
}
