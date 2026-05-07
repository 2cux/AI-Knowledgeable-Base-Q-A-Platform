package com.example.aikb.config;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aikb.common.JwtUtil;
import com.example.aikb.controller.auth.AuthController;
import com.example.aikb.security.JwtAuthenticationFilter;
import com.example.aikb.service.auth.AuthService;
import com.example.aikb.vo.auth.CurrentUserVO;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AuthSecurityCorsTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.cors.allowed-origins[0]=http://localhost:3000",
        "app.cors.allowed-origins[1]=http://127.0.0.1:3000",
        "app.cors.allowed-origins[2]=http://localhost:5173",
        "app.cors.allowed-origins[3]=http://127.0.0.1:5173"
})
class AuthSecurityCorsTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            RabbitAutoConfiguration.class
    })
    @EnableConfigurationProperties(AppCorsProperties.class)
    @Import({AuthController.class, AuthSecurityConfig.class, JwtAuthenticationFilter.class})
    static class TestApplication {
    }

    @Test
    void preflightForAllowedOriginShouldReturnCorsHeadersWithoutToken() throws Exception {
        mockMvc.perform(options("/auth/me")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")));
    }

    @Test
    void preflightForDisallowedOriginShouldNotReturnCorsAllowOrigin() throws Exception {
        mockMvc.perform(options("/auth/me")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void protectedEndpointWithoutTokenShouldStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void protectedEndpointWithBearerTokenShouldAllowCorsRequest() throws Exception {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("userId", Number.class)).thenReturn(7L);
        when(claims.get("username", String.class)).thenReturn("alice");
        when(jwtUtil.parseToken("valid-token")).thenReturn(claims);
        when(authService.getCurrentUser()).thenReturn(CurrentUserVO.builder()
                .userId(7L)
                .username("alice")
                .build());

        mockMvc.perform(get("/auth/me")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }
}
