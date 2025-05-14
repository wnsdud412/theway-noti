package org.silkroadpartnership.theway_noti.user.component;

import org.silkroadpartnership.theway_noti.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    SecurityConfig(UserService userService) {
        this.userService = userService;
    }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/login",
                "/css/**",
                "/js/**",
                "/images/**",
                "/health",
                "/register",
                "/sw.js",
                "manifest.json",
                "/webpush/open-url",
                "/webpush/close-url",
                "/roles")
            .permitAll()
            .requestMatchers(
                "/users/**",
                "/webpush/push-test",
                "/webpush/push",
                "/webpush/push/**")
            .hasRole("ADMIN")
            .anyRequest().authenticated())
        .rememberMe(remember -> remember
            .key("theway-noti-remember-me-key") // 필수: 고유 키
            .rememberMeParameter("remember-me") // HTML form input name
            .tokenValiditySeconds(1209600) // 2주 (기본: 14일)
            .userDetailsService(userService) // 사용자 로드 방식
        )
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/", true)
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout") // 로그아웃 후 이동할 페이지
            .invalidateHttpSession(true) // 세션 무효화
            .clearAuthentication(true)// 인증 정보 제거
            .deleteCookies("JSESSIONID", "remember-me") // 쿠키 삭제
        );
    return http.build();
  }
}
