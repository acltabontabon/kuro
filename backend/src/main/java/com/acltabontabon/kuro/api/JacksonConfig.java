package com.acltabontabon.kuro.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Makes Spring MVC serialize responses through the {@link KuroApiJson} mapper so
 * controller bodies match the @kuro/schemas wire shape. Replaces Boot's default
 * JsonMapper (which backs off on a user-provided bean).
 */
@Configuration
class JacksonConfig {

    @Bean
    JsonMapper jsonMapper() {
        return KuroApiJson.mapper();
    }
}
