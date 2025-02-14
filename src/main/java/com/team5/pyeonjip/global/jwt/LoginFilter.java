package com.team5.pyeonjip.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.pyeonjip.global.exception.ErrorCode;
import com.team5.pyeonjip.global.exception.GlobalException;
import com.team5.pyeonjip.user.dto.CustomUserDetails;
import com.team5.pyeonjip.user.entity.Refresh;
import com.team5.pyeonjip.user.entity.User;
import com.team5.pyeonjip.user.repository.RefreshRepository;
import com.team5.pyeonjip.user.repository.UserRepository;
import com.team5.pyeonjip.user.service.ReissueService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final ReissueService reissueService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshRepository refreshRepository,
                       ReissueService reissueService, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
        this.reissueService = reissueService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        ObjectMapper objectMapper = new ObjectMapper();
        LoginRequest loginRequest;

        try {
            loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException e) {
            throw new GlobalException(ErrorCode.INVALID_LOGIN_REQUEST);
        }

        System.out.println("📌 email: " + loginRequest.getEmail());
        System.out.println("📌 password: " + loginRequest.getPassword());

        // 사용자가 존재하는지 확인
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증 (직접 하지 않고, AuthenticationManager에 맡김)
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());

        System.out.println("✅ 인증 토큰 생성 완료: " + authToken);

        // AuthenticationManager를 통해 인증 진행
        return authenticationManager.authenticate(authToken);
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            String role = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER");

            System.out.println("✅ 로그인 성공: " + email + ", 역할: " + role);

            String access = jwtUtil.createJwt("access", email, role, 60000000L);
            String refresh = jwtUtil.createJwt("refresh", email, role, 86400000L);

            System.out.println("✅ 액세스 토큰: " + access);
            System.out.println("✅ 리프레시 토큰: " + refresh);

            addRefresh(email, refresh, 86400000L);

            response.setHeader("Authorization", "Bearer " + access);
            System.out.println("✅ Authorization 헤더 설정 완료");

            response.addCookie(reissueService.createCookie("refresh", refresh));
            System.out.println("✅ Refresh 토큰 쿠키 설정 완료");

            response.setStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            System.out.println("🚨 로그인 성공 후 처리 중 오류 발생: " + e.getMessage());
            throw new GlobalException(ErrorCode.LOGIN_PROCESSING_ERROR);
        }
    }



    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        throw new GlobalException(ErrorCode.AUTHENTICATION_FAILED);
    }

    private void addRefresh(String email, String refresh, Long expiredMs) {
        try {
            Date date = new Date(System.currentTimeMillis() + expiredMs);
            Refresh newRefresh = new Refresh();
            newRefresh.setEmail(email);
            newRefresh.setRefresh(refresh);
            newRefresh.setExpiration(date.toString());

            System.out.println("✅ Refresh 토큰 저장 시작: " + refresh);
            refreshRepository.save(newRefresh);
            System.out.println("✅ Refresh 토큰 저장 완료");
        } catch (Exception e) {
            System.out.println("🚨 Refresh 토큰 저장 중 오류 발생: " + e.getMessage());
            throw new GlobalException(ErrorCode.LOGIN_PROCESSING_ERROR);
        }
    }

    private static class LoginRequest {
        private String email;
        private String password;

        public LoginRequest() {}

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
