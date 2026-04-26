package com.softserve.api;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Shared base for API integration tests.
 * Uses the already-running PostgreSQL container from docker-compose (localhost:5432/schedule_test).
 * The test application.yml points to schedule_test with user=postgres; we override the password
 * here since the compose container uses password=postgres (not "root" from application.yml).
 * Redis is disabled by @Profile("!test") on CacheConfiguration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "manager@gmail.com", roles = "MANAGER")
@TestPropertySource(properties = {
    "spring.datasource.password=postgres",
    "GOOGLE_CLIENT_ID=disabled",
    "GOOGLE_CLIENT_SECRET=disabled",
    "FACEBOOK_CLIENT_ID=disabled",
    "FACEBOOK_CLIENT_SECRET=disabled"
})
abstract class ApiTestBase {
}
