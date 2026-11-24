package com.example.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:profile-test")
@AutoConfigureMockMvc
class ProfileIntegrationTest {

    private static final String CSRF_HEADER = "X-CSRF-TOKEN";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void registeredUserCanCreateReadUpdateAndDeleteProfile() throws Exception {
        MockHttpSession anonymousSession = new MockHttpSession();
        String csrfToken = fetchCsrfToken(anonymousSession);

        mvc.perform(post("/register")
                        .session(anonymousSession)
                        .header(CSRF_HEADER, csrfToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username": "student", "password": "java12345"}
                                """))
                .andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/login")
                        .session(anonymousSession)
                        .header(CSRF_HEADER, csrfToken)
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .param("username", "student")
                        .param("password", "java12345"))
                .andExpect(status().isNoContent())
                .andReturn();

        // Carry the logged-in session forward and get a fresh token after login.
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        csrfToken = fetchCsrfToken(session);

        mvc.perform(get("/profile").session(session))
                .andExpect(status().isNotFound());

        mvc.perform(post("/profile")
                        .session(session)
                        .header(CSRF_HEADER, csrfToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "01700000000", "address": "Dhaka"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("01700000000"))
                .andExpect(jsonPath("$.address").value("Dhaka"));

        mvc.perform(put("/profile")
                        .session(session)
                        .header(CSRF_HEADER, csrfToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "01700000001", "address": "Chattogram"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("01700000001"))
                .andExpect(jsonPath("$.address").value("Chattogram"));

        mvc.perform(delete("/profile")
                        .session(session)
                        .header(CSRF_HEADER, csrfToken))
                .andExpect(status().isNoContent());

        mvc.perform(get("/profile").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void loggedOutUserCannotReadProfile() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());
    }

    private String fetchCsrfToken(MockHttpSession session) throws Exception {
        String response = mvc.perform(get("/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json.readTree(response).get("token").asText();
    }
}
