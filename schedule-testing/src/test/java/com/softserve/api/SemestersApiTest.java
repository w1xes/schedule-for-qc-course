package com.softserve.api;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Task 2.2: Integration tests for the Semester resource (additional resource).
 * Task 2.3: DB verification tests (Orders 4 and 11).
 *
 * Semester is an "additional" resource: it references Period via ManyToMany (semester_period table).
 * Since fillDefaultValues() fetches available periods from DB (and can return empty set),
 * semesters can be created with semester_classes: [] — @NotNull on periods allows empty Set.
 * Semester DELETE is a hard delete (row removed from DB).
 *
 * Date format used by API: "dd/MM/yyyy" (e.g. "01/09/2026").
 * Negative test for invalid dates: endDay before startDay → IncorrectTimeException → 400.
 */
@Tag("api")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SemestersApiTest extends ApiTestBase {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long createdSemesterId;
    String createdDescription;

    @BeforeAll
    void setUp() {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        RestAssuredMockMvc.mockMvc(mockMvc);

        jdbcTemplate.execute(
                "TRUNCATE semester_period, semester_group, semester_day, schedules, lessons, semesters RESTART IDENTITY CASCADE"
        );
    }

    // ------------------------------------------------------------------
    // Order 1: GET all — 200, JSON array
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("GET /semesters - should return 200 and a JSON array")
    void shouldReturnListOfSemesters() {
        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/semesters")
                .then()
                .statusCode(200)
                .body("$", instanceOf(List.class));
    }

    // ------------------------------------------------------------------
    // Order 2: GET all with seeded data — check required fields
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("GET /semesters - each item must have id, description, year when non-empty")
    void shouldReturnSemestersWithRequiredFields() {
        jdbcTemplate.execute(
                "INSERT INTO semesters(description, year, start_day, end_day, "
                + "current_semester, default_semester, disable) "
                + "VALUES ('FieldCheck_Sem', 2025, '2025-02-01', '2025-06-30', false, false, false)"
        );

        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/semesters")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].description", notNullValue())
                .body("[0].year", notNullValue());

        jdbcTemplate.execute("DELETE FROM semesters WHERE description = 'FieldCheck_Sem'");
    }

    // ------------------------------------------------------------------
    // Order 3: POST create — 201 + save id
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("POST /semesters - should create semester and return 201 with id")
    void shouldCreateNewSemester() {
        createdDescription = "Semester_" + System.currentTimeMillis();

        String body = "{"
                + "\"description\":\"" + createdDescription + "\","
                + "\"year\":2026,"
                + "\"startDay\":\"01/09/2026\","
                + "\"endDay\":\"31/12/2026\","
                + "\"currentSemester\":false,"
                + "\"defaultSemester\":false,"
                + "\"semester_days\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\"],"
                + "\"semester_classes\":[],"
                + "\"semester_groups\":[]"
                + "}";

        Integer id = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .post("/semesters")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("description", equalTo(createdDescription))
                .body("year", equalTo(2026))
                .extract().path("id");

        createdSemesterId = id.longValue();
    }

    // ------------------------------------------------------------------
    // Order 4 (Task 2.3): DB verification — row must exist after POST
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("DB: semester created via POST must be persisted in the database")
    void shouldVerifySemesterSavedToDatabase() {
        assertNotNull(createdSemesterId, "Requires Order 3 to run first");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM semesters WHERE id = ?", createdSemesterId
        );

        assertEquals(1, rows.size(), "Exactly one row must exist in DB");
        assertEquals(createdDescription, rows.get(0).get("description"), "description must match");
        assertEquals(2026, rows.get(0).get("year"), "year must be 2026");
    }

    // ------------------------------------------------------------------
    // Order 5: GET by ID — 200 with all key fields
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("GET /semesters/{id} - should return 200 with the correct semester")
    void shouldReturnSemesterById() {
        assertNotNull(createdSemesterId, "Requires Order 3 to run first");

        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/semesters/{id}", String.valueOf(createdSemesterId))
                .then()
                .statusCode(200)
                .body("id", equalTo(createdSemesterId.intValue()))
                .body("description", equalTo(createdDescription))
                .body("year", equalTo(2026));
    }

    // ------------------------------------------------------------------
    // Order 6 (negative): GET non-existing — 404
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("GET /semesters/999999 - should return 404 for non-existing id (negative)")
    void shouldReturn404ForNonExistingSemester() {
        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/semesters/999999")
                .then()
                .statusCode(404);
    }

    // ------------------------------------------------------------------
    // Order 7 (negative): POST with endDay < startDay — 400
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("POST /semesters with endDay before startDay - should return 400 (negative)")
    void shouldReturn400ForInvalidDates() {
        String description = "Invalid_" + System.currentTimeMillis();

        String body = "{"
                + "\"description\":\"" + description + "\","
                + "\"year\":2026,"
                + "\"startDay\":\"01/12/2026\","
                + "\"endDay\":\"01/09/2026\","
                + "\"currentSemester\":false,"
                + "\"defaultSemester\":false,"
                + "\"semester_days\":[\"MONDAY\",\"TUESDAY\"],"
                + "\"semester_classes\":[],"
                + "\"semester_groups\":[]"
                + "}";

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .post("/semesters")
                .then()
                .statusCode(400);
    }

    // ------------------------------------------------------------------
    // Order 8: PUT update — 200 with updated description
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("PUT /semesters - should update semester description and return 200")
    void shouldUpdateSemester() {
        assertNotNull(createdSemesterId, "Requires Order 3 to run first");

        String updatedDescription = "Updated_Semester_" + System.currentTimeMillis();

        String body = "{"
                + "\"id\":" + createdSemesterId + ","
                + "\"description\":\"" + updatedDescription + "\","
                + "\"year\":2026,"
                + "\"startDay\":\"01/09/2026\","
                + "\"endDay\":\"31/12/2026\","
                + "\"currentSemester\":false,"
                + "\"defaultSemester\":false,"
                + "\"semester_days\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\"],"
                + "\"semester_classes\":[],"
                + "\"semester_groups\":[]"
                + "}";

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .put("/semesters")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdSemesterId.intValue()))
                .body("description", equalTo(updatedDescription));

        createdDescription = updatedDescription;
    }

    // ------------------------------------------------------------------
    // Order 9: DELETE — 200/204
    // ------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("DELETE /semesters/{id} - should delete semester and return 200/204")
    void shouldDeleteSemester() {
        assertNotNull(createdSemesterId, "Requires Order 3 to run first");

        given()
                .when()
                .delete("/semesters/{id}", String.valueOf(createdSemesterId))
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    // ------------------------------------------------------------------
    // Order 10: GET after DELETE — 404
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("GET /semesters/{id} after DELETE - should return 404")
    void shouldReturn404AfterDeletion() {
        assertNotNull(createdSemesterId, "Requires Order 9 to run first");

        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/semesters/{id}", String.valueOf(createdSemesterId))
                .then()
                .statusCode(404);
    }

    // ------------------------------------------------------------------
    // Order 11 (Task 2.3): DB verification — row must be gone after DELETE
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("DB: semester deleted via DELETE must no longer exist in the database")
    void shouldVerifySemesterRemovedFromDatabase() {
        assertNotNull(createdSemesterId, "Requires Order 9 to run first");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM semesters WHERE id = ?", createdSemesterId
        );

        assertEquals(0, rows.size(), "Hard-deleted semester must not exist in DB");
    }
}
