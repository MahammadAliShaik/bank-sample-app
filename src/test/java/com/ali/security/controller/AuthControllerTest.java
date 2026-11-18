package com.ali.security.controller;

import com.ali.security.dto.AuthRequest;
import com.ali.security.dto.RegisterRequest;
import com.ali.security.entity.UserInfo;
import com.ali.security.repository.UserInfoRepository;
import com.ali.security.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController controller;

    @Test
    void registrationHashesPasswordAndAlwaysAssignsCustomerRole() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("customer@example.com");
        request.setName("Customer");
        request.setPassword("a-long-password");
        given(userInfoRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("hashed-password");

        ResponseEntity<String> response = controller.register(request);

        ArgumentCaptor<UserInfo> userCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoRepository).save(userCaptor.capture());
        UserInfo saved = userCaptor.getValue();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(saved.getEmail()).isEqualTo(request.getEmail());
        assertThat(saved.getName()).isEqualTo("Customer");
        assertThat(saved.getPassword()).isEqualTo("hashed-password");
        assertThat(saved.getRoles()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void duplicateEmailReturnsConflictWithoutSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("customer@example.com");
        request.setName("Customer");
        request.setPassword("a-long-password");
        given(userInfoRepository.existsByEmail(request.getEmail())).willReturn(true);

        assertThatThrownBy(() -> controller.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginReturnsJwtOnlyAfterSuccessfulAuthentication() {
        AuthRequest request = new AuthRequest();
        request.setEmail("customer@example.com");
        request.setPassword("a-long-password");
        given(authenticationManager.authenticate(any()))
                .willReturn(UsernamePasswordAuthenticationToken.authenticated(
                        request.getEmail(), null, List.of()));
        given(jwtService.generateToken(request.getEmail())).willReturn("signed-token");

        assertThat(controller.authenticateAndGetToken(request)).isEqualTo("signed-token");
    }
}
