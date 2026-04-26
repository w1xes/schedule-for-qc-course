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
 * Task 2.1: CRUD integration tests for the Teacher resource (primary resource).
 * Task 2.3: DB verification tests are included (Orders 4 and 10).
 */
@Tag("api")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeachersApiTest extends ApiTestBase {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long createdTeacherId;

    @BeforeAll
    void setUp() {
        MockMvc mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        RestAssuredMockMvc.mockMvc(mockMvc);

        jdbcTemplate.execute(
            "TRUNCATE lessons, teachers, department, users RESTART IDENTITY CASCADE"
        );
        jdbcTemplate.execute(
            "INSERT INTO department(id, name) VALUES (1, 'Test Department')"
        );
    }

    @Test
    @Order(1)
    @DisplayName("GET /teachers - should return 200 and a JSON array")
    void shouldReturnListOfTeachers() {
        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/teachers")
            .then()
            .statusCode(200)
            .body("$", instanceOf(List.class));
    }

    @Test
    @Order(2)
    @DisplayName("GET /teachers - each item must have id, name, surname when list is non-empty")
    void shouldReturnTeachersWithRequiredFields() {
        jdbcTemplate.execute(
            "INSERT INTO teachers(name, surname, patronymic, position, disable) "
            + "VALUES ('FieldOk', 'Validator', 'Testovych', 'асистент', false)"
        );

        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/teachers")
            .then()
            .statusCode(200)
            .body("[0].id", notNullValue())
            .body("[0].name", notNullValue())
            .body("[0].surname", notNullValue());

        jdbcTemplate.execute("DELETE FROM teachers WHERE name = 'FieldOk'");
    }

    @Test
    @Order(3)
    @DisplayName("POST /teachers - should create teacher and return 201 with id")
    void shouldCreateNewTeacher() {
        String body = String.format(
            "{\"name\":\"Test\",\"surname\":\"Teacher_%d\","
            + "\"patronymic\":\"Testovych\",\"position\":\"доцент\",\"disable\":false}",
            System.currentTimeMillis()
        );

        Integer id = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .when()
            .post("/teachers")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Test"))
            .body("position", equalTo("доцент"))
            .extract().path("id");

        createdTeacherId = id.longValue();
    }

    @Test
    @Order(4)
    @DisplayName("DB: teacher created via POST must be persisted in the database")
    void shouldVerifyTeacherSavedToDatabase() {
        assertNotNull(createdTeacherId, "Requires Order 3 to run first");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM teachers WHERE id = ?", createdTeacherId
        );

        assertEquals(1, rows.size(), "Exactly one row must exist in DB");
        assertEquals("Test", rows.get(0).get("name"), "name must match");
        assertEquals("доцент", rows.get(0).get("position"), "position must match");
    }

    @Test
    @Order(5)
    @DisplayName("GET /teachers/{id} - should return 200 with the correct teacher")
    void shouldReturnTeacherById() {
        assertNotNull(createdTeacherId, "Requires Order 3 to run first");

        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/teachers/{id}", String.valueOf(createdTeacherId))
            .then()
            .statusCode(200)
            .body("id", equalTo(createdTeacherId.intValue()))
            .body("name", equalTo("Test"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /teachers/999999 - should return 404 for non-existing id (negative)")
    void shouldReturn404ForNonExistingTeacher() {
        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/teachers/999999")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    @DisplayName("POST /teachers with empty fields - should return 400 (negative)")
    void shouldReturn400ForInvalidTeacherData() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("{\"name\":\"\",\"surname\":\"\",\"patronymic\":\"\",\"position\":\"\"}")
            .when()
            .post("/teachers")
            .then()
            .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("PUT /teachers - should update teacher and return 200 with updated data")
    void shouldUpdateTeacher() {
        assertNotNull(createdTeacherId, "Requires Order 3 to run first");

        String body = String.format(
            "{\"id\":%d,\"name\":\"Updated\",\"surname\":\"Teacher\","
            + "\"patronymic\":\"Testovych\",\"position\":\"професор\",\"disable\":false}",
            createdTeacherId
        );

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .when()
            .put("/teachers")
            .then()
            .statusCode(200)
            .body("id", equalTo(createdTeacherId.intValue()))
            .body("name", equalTo("Updated"))
            .body("position", equalTo("професор"));
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /teachers/{id} - should delete teacher and return 200/204")
    void shouldDeleteTeacher() {
        assertNotNull(createdTeacherId, "Requires Order 3 to run first");

        given()
            .when()
            .delete("/teachers/{id}", String.valueOf(createdTeacherId))
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Test
    @Order(10)
    @DisplayName("GET /teachers/{id} after DELETE - should return 404")
    void shouldReturn404AfterDeletion() {
        assertNotNull(createdTeacherId, "Requires Order 9 to run first");

        given()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/teachers/{id}", String.valueOf(createdTeacherId))
            .then()
            .statusCode(404);
    }
}
