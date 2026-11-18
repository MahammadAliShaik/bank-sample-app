package com.ali.security.service;

import com.ali.security.entity.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfoToUserDetailsTest {

    @Test
    void convertsTrimmedCommaSeparatedRolesIntoAuthorities() {
        UserInfo user = new UserInfo();
        user.setEmail("customer@example.com");
        user.setPassword("hashed");
        user.setRoles("ROLE_CUSTOMER, ROLE_ADMIN");

        UserInfoToUserDetails details = new UserInfoToUserDetails(user);

        assertThat(details.getUsername()).isEqualTo("customer@example.com");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void missingRolesDoNotCauseAnExceptionOrGrantAuthority() {
        UserInfo user = new UserInfo();
        user.setEmail("customer@example.com");

        assertThat(new UserInfoToUserDetails(user).getAuthorities()).isEmpty();
    }
}
