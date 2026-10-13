package com.loopcycle.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.loopcycle.auth.TokenService;
import com.loopcycle.auth.UserRepository;
import com.loopcycle.notification.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ItemRepository repository;
    @MockBean LifecycleService lifecycle;
    @MockBean NotificationService notifications;
    @MockBean TokenService tokenService;
    @MockBean UserRepository users;
    @MockBean PasswordEncoder passwords;

    @Test
    @WithMockUser(username = "citizen@example.com", roles = "CITIZEN")
    void createsSubmissionAndInitialTraceEvent() throws Exception {
        when(repository.save(any(EwasteItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/items").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\",\"category\":\"Computers and laptops\",\"weightKg\":2.4,\"address\":\"Demo address\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.history[0].toStatus").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "citizen@example.com", roles = "CITIZEN")
    void rejectsNonPositiveWeight() throws Exception {
        mvc.perform(post("/api/items").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\",\"category\":\"Computers\",\"weightKg\":0,\"address\":\"Demo address\"}"))
                .andExpect(status().isBadRequest());
    }
}
