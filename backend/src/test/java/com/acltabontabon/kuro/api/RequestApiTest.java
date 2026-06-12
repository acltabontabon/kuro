package com.acltabontabon.kuro.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequestApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static final String SUBJECT = """
            "subject": {"kind": "employer", "displayName": "Acme Corp"}""";

    @Test
    void unsupportedCategoryReturnsTwoHundredRefusalNotAnError() throws Exception {
        String body = "{\"category\": \"healthcare\", " + SUBJECT + "}";

        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSufficiency").value("unsupported_category"))
                .andExpect(jsonPath("$.requestedCategory").value("healthcare"))
                .andExpect(jsonPath("$.supportedCategories[0]").value("employment_intelligence"))
                .andExpect(jsonPath("$.supportedCategories[1]").value("rental_intelligence"))
                .andExpect(jsonPath("$.supportScore").doesNotExist());
    }

    @Test
    void supportedCategoryCreatesRequestWithNoResultYet() throws Exception {
        String body = "{\"category\": \"employment_intelligence\", " + SUBJECT + "}";

        String response = mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");

        mvc.perform(get("/api/requests/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.subjectDisplayName").value("Acme Corp"));
        mvc.perform(get("/api/requests/{id}/result", id)).andExpect(status().isConflict());
        mvc.perform(get("/api/requests/{id}/evidence", id)).andExpect(status().isConflict());
    }

    @Test
    void unknownRequestIsNotFound() throws Exception {
        mvc.perform(get("/api/requests/{id}", "does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void historyListsCreatedRequests() throws Exception {
        String body = "{\"category\": \"rental_intelligence\", \"subject\": {\"kind\": \"rental\", "
                + "\"displayName\": \"Maple Court Apartments\"}}";
        String id = JsonPath.read(mvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void attachSourceRequiresExactlyOneOfUrlOrText() throws Exception {
        String body = "{\"category\": \"employment_intelligence\", " + SUBJECT + "}";
        String id = JsonPath.read(mvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/requests/{id}/sources", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://example.com/review\"}")).andExpect(status().isCreated());
        mvc.perform(post("/api/requests/{id}/sources", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://x\", \"text\": \"also\"}")).andExpect(status().isBadRequest());
    }
}
