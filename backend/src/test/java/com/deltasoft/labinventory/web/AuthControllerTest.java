package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.domain.AppUser;
import com.deltasoft.labinventory.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String USERNAME = "yash.s";
    private static final String PASSWORD = "labtech";

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void seedUser() {
        users.deleteAll();
        users.save(new AppUser(USERNAME, encoder.encode(PASSWORD), "Lab Tech", "LAB_TECH"));
    }

    @Test
    void anonymousReagentRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/api/reagents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void loginWithBadPasswordReturns401WithMessage() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", USERNAME,
                                "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid username or password")));
    }

    @Test
    void loginWithGoodCredsReturnsBodyAndPopulatesSession() throws Exception {
        MvcResult res = loginSuccess();
        // MockMvc doesn't always emit the JSESSIONID cookie on the response, but the
        // session itself carries the security context — which is what subsequent
        // requests rely on. Assert the session was created and the context attached.
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);
        assertNotNull(session, "login should create an HTTP session");
        assertNotNull(
                session.getAttribute("SPRING_SECURITY_CONTEXT"),
                "session should carry the SecurityContext attribute");
    }

    @Test
    void sessionCookieAuthorisesSubsequentReagentRequests() throws Exception {
        MockHttpSession session = sessionFromLogin();
        mvc.perform(get("/api/reagents").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    void meReturnsAuthenticatedUserShape() throws Exception {
        MockHttpSession session = sessionFromLogin();
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(USERNAME)))
                .andExpect(jsonPath("$.displayName", is("Lab Tech")))
                .andExpect(jsonPath("$.role", is("LAB_TECH")));
    }

    @Test
    void httpBasicAuthorisesReagentRequests() throws Exception {
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString((USERNAME + ":" + PASSWORD).getBytes());
        mvc.perform(get("/api/reagents").header(HttpHeaders.AUTHORIZATION, basic))
                .andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        MockHttpSession session = sessionFromLogin();
        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        // Same MockHttpSession instance — after invalidation it's marked invalid and
        // Spring filters reject the next request as unauthenticated.
        mvc.perform(get("/api/reagents").session(session))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult loginSuccess() throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", USERNAME,
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(USERNAME)))
                .andExpect(jsonPath("$.displayName", is("Lab Tech")))
                .andReturn();
    }

    private MockHttpSession sessionFromLogin() throws Exception {
        MockHttpSession session = (MockHttpSession) loginSuccess().getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }
}
