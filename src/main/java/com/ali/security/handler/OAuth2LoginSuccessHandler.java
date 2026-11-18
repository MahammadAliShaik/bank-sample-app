package com.ali.security.handler;

import com.ali.security.entity.UserInfo;
import com.ali.security.repository.UserInfoRepository;
import com.ali.security.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserInfoRepository userInfoRepository;

    public OAuth2LoginSuccessHandler(JwtService jwtService, UserInfoRepository userInfoRepository) {
        this.jwtService = jwtService;
        this.userInfoRepository = userInfoRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email") != null ? oAuth2User.getAttribute("email") : oAuth2User.getAttribute("login");

        if (userInfoRepository.findByEmail(email).isEmpty()) {
            UserInfo newUser = new UserInfo();
            newUser.setEmail(email);
            newUser.setName(oAuth2User.getAttribute("name"));
            newUser.setRoles("ROLE_CUSTOMER");
            userInfoRepository.save(newUser);
        }

        String token = jwtService.generateToken(email);
        response.setContentType("application/json");
        response.getWriter().write("{\"token\": \"" + token + "\"}");
    }
}