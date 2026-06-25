package com.smartscheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Taskay API")
                        .version("1.0.0")
                        .description("""
                                ## Taskay — Intelligent Task Priority Engine
                                
                                REST API that automatically calculates a priority score for each task
                                using urgency, importance, and deadline proximity.
                                
                                ### Priority Formula
                                ```
                                score = (urgency × 0.35) + (importance × 0.35) + (deadlineScore × 0.30)
                                ```
                                
                                ### Priority Levels
                                | Score | Level    | Action          |
                                |-------|----------|-----------------|
                                | ≥ 8.0 | CRITICAL | Do immediately  |
                                | ≥ 6.0 | HIGH     | Schedule today  |
                                | ≥ 4.0 | MEDIUM   | Plan this week  |
                                | < 4.0 | LOW      | Delegate/Defer  |
                                """)
                        .contact(new Contact()
                                .name("Taskay")
                                .url("https://github.com/Julhaalmeida/taskay"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
