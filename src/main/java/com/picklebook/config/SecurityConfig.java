package com.picklebook.config;

import com.picklebook.security.CustomUserDetailsService;
import com.picklebook.security.LoginAttemptService;
import com.picklebook.security.LoginFailureHandler;
import com.picklebook.security.LoginRateLimitFilter;
import com.picklebook.security.LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình bảo mật:
 * <ul>
 *   <li>Đăng nhập bằng form, mật khẩu BCrypt.</li>
 *   <li>Phân quyền theo vai trò: ADMIN toàn quyền, STAFF chỉ xử lý đơn.</li>
 *   <li>CSRF bật cho toàn bộ POST, trừ H2 console ở môi trường dev.</li>
 *   <li>Giới hạn số lần đăng nhập sai, chống session fixation.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final LoginAttemptService loginAttemptService;
    private final RequestCache requestCache;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          LoginSuccessHandler loginSuccessHandler,
                          LoginFailureHandler loginFailureHandler,
                          LoginAttemptService loginAttemptService,
                          RequestCache requestCache) {
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.loginAttemptService = loginAttemptService;
        this.requestCache = requestCache;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Tài nguyên tĩnh & trang công khai
                        .requestMatchers("/main.css", "/main.js", "/css/**", "/js/**", "/img/**",
                                "/vendor/**", "/uploads/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/san", "/san/**", "/api/courts/**", "/tin-tuc", "/tin-tuc/**",
                                "/gioi-thieu", "/chinh-sach", "/lien-he",
                                "/dat-co-dinh", "/dat-co-dinh/**", "/api/recurring/**", "/api/bookings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/dat-san/thanh-cong/**").permitAll()
                        .requestMatchers("/dang-nhap", "/dang-ky", "/quen-mat-khau", "/dat-lai-mat-khau").permitAll()
                        .requestMatchers("/quan-ly-dat-san/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/403", "/404", "/500", "/error").permitAll()
                        // Khu vực quản trị
                        .requestMatchers("/admin/users/**", "/admin/settings/**", "/admin/pricing/**",
                                "/admin/coupons/**", "/admin/services/**", "/admin/email-templates/**",
                                "/admin/questions/**", "/admin/audit-logs/**", "/admin/courts/**",
                                "/admin/blocked-times/**",
                                "/admin/banners/**", "/admin/news/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                        // Cần đăng nhập
                        .requestMatchers(HttpMethod.POST, "/dat-san", "/api/booking/**").authenticated()
                        .requestMatchers("/lich-cua-toi/**", "/tai-khoan/**", "/api/booking/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .requestCache(cache -> cache.requestCache(requestCache))
                .logout(logout -> logout
                        .logoutUrl("/dang-xuat")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false))
                .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "img-src 'self' data: https:; "
                                        + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                                        + "font-src 'self' data: https://fonts.gstatic.com; "
                                        + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                                        + "connect-src 'self'; "
                                        + "frame-src 'self';")));

        http.addFilterBefore(new LoginRateLimitFilter(loginAttemptService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
