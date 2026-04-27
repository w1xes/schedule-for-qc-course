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
 * Task 2.1: CRUD integration tests for the Group resource (primary resource).
 * Task 2.3: DB verification tests (Orders 4 and 10).
 *
 * Group fields: id, title, disable, sortOrder.
 * Group DELETE is a hard delete (row removed from DB).
 */
@Tag("api")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GroupsApiTest extends ApiTestBase {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long createdGroupId;
    String createdGroupTitle;

    @BeforeAll
    void setUp() {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        RestAssuredMockMvc.mockMvc(mockMvc);

        jdbcTemplate.execute(
                "TRUNCATE lessons, semester_group, groups RESTART IDENTITY CASCADE"
        );
    }

    // ------------------------------------------------------------------
    // Order 1: GET all — empty list, 200
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("GET /groups - should return 200 and a JSON array")
    void shouldReturnListOfGroups() {
        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/groups")
                .then()
                .statusCode(200)
                .body("$", instanceOf(List.class));
    }

    // ------------------------------------------------------------------
    // Order 2: GET all with seed data — check required fields
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("GET /groups - each item must have id and title when list is non-empty")
    void shouldReturnGroupsWithRequiredFields() {
        jdbcTemplate.execute(
                "INSERT INTO groups(title, disable) VALUES ('FieldCheck_Group', false)"
        );

        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/groups")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].title", notNullValue());

        jdbcTemplate.execute("DELETE FROM groups WHERE title = 'FieldCheck_Group'");
    }

    // ------------------------------------------------------------------
    // Order 3: POST create — 201 + save id for subsequent tests
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("POST /groups - should create group and return 201 with id")
    void shouldCreateNewGroup() {
        createdGroupTitle = "TestGroup_" + System.currentTimeMillis();

        Integer id = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"title\":\"" + createdGroupTitle + "\",\"disable\":false}")
                .when()
                .post("/groups")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo(createdGroupTitle))
                .body("disable", equalTo(false))
                .extract().path("id");

        createdGroupId = id.longValue();
    }

    // ------------------------------------------------------------------
    // Order 4 (Task 2.3): DB verification — row must exist after POST
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("DB: group created via POST must be persisted in the database")
    void shouldVerifyGroupSavedToDatabase() {
        assertNotNull(createdGroupId, "Requires Order 3 to run first");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM groups WHERE id = ?", createdGroupId
        );

        assertEquals(1, rows.size(), "Exactly one row must exist in DB");
        assertEquals(createdGroupTitle, rows.get(0).get("title"), "title must match");
        assertEquals(false, rows.get(0).get("disable"), "disable must be false");
    }

    // ------------------------------------------------------------------
    // Order 5: GET by ID — 200 with correct data
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("GET /groups/{id} - should return 200 with the correct group")
    void shouldReturnGroupById() {
        assertNotNull(createdGroupId, "Requires Order 3 to run first");

        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/groups/{id}", String.valueOf(createdGroupId))
                .then()
                .statusCode(200)
                .body("id", equalTo(createdGroupId.intValue()))
                .body("title", equalTo(createdGroupTitle));
    }

    // ------------------------------------------------------------------
    // Order 6 (negative): GET non-existing — 404
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("GET /groups/999999 - should return 404 for non-existing id (negative)")
    void shouldReturn404ForNonExistingGroup() {
        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/groups/999999")
                .then()
                .statusCode(404);
    }

    // ------------------------------------------------------------------
    // Order 7 (negative): POST with empty title — 400
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("POST /groups with empty title - should return 400 (negative)")
    void shouldReturn400ForEmptyTitle() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"title\":\"\",\"disable\":false}")
                .when()
                .post("/groups")
                .then()
                .statusCode(400);
    }

    // ------------------------------------------------------------------
    // Order 8: PUT update — 200 with updated title
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("PUT /groups - should update group title and return 200")
    void shouldUpdateGroup() {
        assertNotNull(createdGroupId, "Requires Order 3 to run first");

        String updatedTitle = "Updated_" + System.currentTimeMillis();

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"id\":" + createdGroupId + ",\"title\":\"" + updatedTitle + "\",\"disable\":false}")
                .when()
                .put("/groups")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdGroupId.intValue()))
                .body("title", equalTo(updatedTitle));

        createdGroupTitle = updatedTitle;
    }

    // ------------------------------------------------------------------
    // Order 9: DELETE — 200/204
    // ------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("DELETE /groups/{id} - should delete group and return 200/204")
    void shouldDeleteGroup() {
        assertNotNull(createdGroupId, "Requires Order 3 to run first");

        given()
                .when()
                .delete("/groups/{id}", String.valueOf(createdGroupId))
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    // ------------------------------------------------------------------
    // Order 10: GET after DELETE — 404
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("GET /groups/{id} after DELETE - should return 404")
    void shouldReturn404AfterDeletion() {
        assertNotNull(createdGroupId, "Requires Order 9 to run first");

        given()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/groups/{id}", String.valueOf(createdGroupId))
                .then()
                .statusCode(404);
    }

    // ------------------------------------------------------------------
    // Order 11 (Task 2.3): DB verification — row must be gone after DELETE
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("DB: group deleted via DELETE must no longer exist in the database")
    void shouldVerifyGroupRemovedFromDatabase() {
        assertNotNull(createdGroupId, "Requires Order 9 to run first");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM groups WHERE id = ?", createdGroupId
        );

        assertEquals(0, rows.size(), "Hard-deleted group must not exist in DB");
    }
}
