package com.ali.security.filter;

import com.ali.security.service.JwtService;
import com.ali.security.service.MyUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final MyUserDetailsService userDetailsService = mock(MyUserDetailsService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService, redisTemplate);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesTheUser() throws Exception {
        Claims claims = claims("customer@example.com", "token-id");
        given(jwtService.extractAllClaims("valid-token")).willReturn(claims);
        given(redisTemplate.hasKey("blacklist:token-id")).willReturn(false);
        given(userDetailsService.loadUserByUsername("customer@example.com"))
                .willReturn(User.withUsername("customer@example.com").password("unused").authorities("ROLE_CUSTOMER").build());
        given(jwtService.validateToken(claims, "customer@example.com")).willReturn(true);

        MockHttpServletRequest request = request("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("customer@example.com");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void malformedTokenReturnsUnauthorizedInsteadOfServerError() throws Exception {
        given(jwtService.extractAllClaims("invalid-token"))
                .willThrow(new MalformedJwtException("Invalid token"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("invalid-token"), response, (req, res) -> {
            throw new AssertionError("Filter chain must not run for an invalid token");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
    }

    @Test
    void revokedTokenReturnsUnauthorized() throws Exception {
        Claims tokenClaims = claims("customer@example.com", "revoked-id");
        given(jwtService.extractAllClaims("revoked-token")).willReturn(tokenClaims);
        given(redisTemplate.hasKey("blacklist:revoked-id")).willReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("revoked-token"), response, (req, res) -> {
            throw new AssertionError("Filter chain must not run for a revoked token");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private Claims claims(String username, String tokenId) {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(username);
        given(claims.getId()).willReturn(tokenId);
        return claims;
    }
}
