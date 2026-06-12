package com.acltabontabon.kuro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Boots the full context against SQLite — this is the executable viability
 * check for the SQLite + Hibernate 7 + Flyway stack (see issue #9).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KuroApplicationTests {

    @Value("${local.server.port}")
    int port;

    private final RestClient restClient = RestClient.create();

    @Test
    void healthIsUp() {
        var response = restClient.get()
                .uri("http://localhost:" + port + "/actuator/health")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void onlyHealthAndInfoAreExposed() {
        var notFound = catchThrowableOfType(HttpClientErrorException.NotFound.class, () ->
                restClient.get()
                        .uri("http://localhost:" + port + "/actuator/metrics")
                        .retrieve()
                        .toBodilessEntity());

        assertThat(notFound).isNotNull();
    }
}
