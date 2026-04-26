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
 * Task 2.2: Integration tests for the Subject resource (additional resource).
 * Task 2.3: DB verification tests (Orders 4 and 11).
 */
@Tag("api")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubjectsApiTest extends ApiTestBase {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long createdSubjectId;

    @BeforeAll
    void setUp() {
        MockMvc mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        RestAssuredMockMvc.mockMvc(mockMvc);

        jdbcTemplate.execute("TRUNCATE lessons, subjects RESTART IDENTITY CASCADE");
    }

    @Test
    @Order(1)
    @DisplayName("GET /subjects - should return 200 and a JSON array")
    void shouldReturnListOfSubjects() {
        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/subjects")
            .then()
            .statusCode(200)
            .body("$", instanceOf(List.class));
    }

    @Test
    @Order(2)
    @DisplayName("GET /subjects - each item must have id and name when list is non-empty")
    void shouldReturnSubjectsWithRequiredFields() {
        jdbcTemplate.execute(
            "INSERT INTO subjects(name, disable) VALUES ('FieldCheckSubject', false)"
        );

        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/subjects")
            .then()
            .statusCode(200)
            .body("[0].id", notNullValue())
            .body("[0].name", notNullValue());

        jdbcTemplate.execute("DELETE FROM subjects WHERE name = 'FieldCheckSubject'");
    }

    @Test
    @Order(3)
    @DisplayName("POST /subjects - should create subject and return 201 with id")
    void shouldCreateNewSubject() {
        String body = String.format(
            "{\"name\":\"Subject_%d\",\"disable\":false}",
            System.currentTimeMillis()
        );

        Integer id = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .when()
            .post("/subjects")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("disable", equalTo(false))
            .extract().path("id");

        createdSubjectId = id.longValue();
    }

    @Test
    @Order(4)
    @DisplayName("DB: subject created via POST must be persisted in the database")
    void shouldVerifySubjectSavedToDatabase() {
        assertNotNull(createdSubjectId, "Requires Order 3 to run first");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM subjects WHERE id = ?", createdSubjectId
        );

        assertEquals(1, rows.size(), "Exactly one row must exist in DB");
        assertEquals(false, rows.get(0).get("disable"), "disable must be false in DB");
    }

    @Test
    @Order(5)
    @DisplayName("GET /subjects/{id} - should return 200 with the correct subject")
    void shouldReturnSubjectById() {
        assertNotNull(createdSubjectId, "Requires Order 3 to run first");

        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/subjects/{id}", String.valueOf(createdSubjectId))
            .then()
            .statusCode(200)
            .body("id", equalTo(createdSubjectId.intValue()));
    }

    @Test
    @Order(6)
    @DisplayName("GET /subjects/999999 - should return 404 for non-existing id (negative)")
    void shouldReturn404ForNonExistingSubject() {
        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/subjects/999999")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    @DisplayName("POST /subjects with empty name - should return 400 (negative)")
    void shouldReturn400ForInvalidSubjectData() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("{\"name\":\"\",\"disable\":false}")
            .when()
            .post("/subjects")
            .then()
            .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("PUT /subjects - should update subject name and return 200")
    void shouldUpdateSubject() {
        assertNotNull(createdSubjectId, "Requires Order 3 to run first");

        String updatedName = "UpdatedSubject_" + System.currentTimeMillis();
        String body = String.format(
            "{\"id\":%d,\"name\":\"%s\",\"disable\":false}",
            createdSubjectId, updatedName
        );

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .when()
            .put("/subjects")
            .then()
            .statusCode(200)
            .body("id", equalTo(createdSubjectId.intValue()))
            .body("name", equalTo(updatedName));
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /subjects/{id} - should delete subject and return 200/204")
    void shouldDeleteSubject() {
        assertNotNull(createdSubjectId, "Requires Order 3 to run first");

        given()
            .when()
            .delete("/subjects/{id}", String.valueOf(createdSubjectId))
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Test
    @Order(10)
    @DisplayName("GET /subjects/{id} after DELETE - should return 404")
    void shouldReturn404AfterDeletion() {
        assertNotNull(createdSubjectId, "Requires Order 9 to run first");

        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/subjects/{id}", String.valueOf(createdSubjectId))
            .then()
            .statusCode(404);
    }

    @Test
    @Order(11)
    @DisplayName("DB: subject deleted via DELETE must no longer exist in the database")
    void shouldVerifySubjectRemovedFromDatabase() {
        assertNotNull(createdSubjectId, "Requires Order 9 to run first");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM subjects WHERE id = ?", createdSubjectId
        );

        assertEquals(0, rows.size(), "Hard-deleted subject must not exist in DB");
    }
}
