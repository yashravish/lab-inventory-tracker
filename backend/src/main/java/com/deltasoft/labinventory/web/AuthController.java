package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.repository.AppUserRepository;
import com.deltasoft.labinventory.web.dto.AuthMe;
import com.deltasoft.labinventory.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;

    public AuthController(AuthenticationManager authenticationManager, AppUserRepository users) {
        this.authenticationManager = authenticationManager;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        String username = body.getUsername();
        String password = body.getPassword();
        if (username == null || username.isBlank() || password == null) {
            return unauthorizedBody("Invalid username or password.");
        }
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username.trim(), password));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            return ResponseEntity.ok(meBody(auth.getName()));
        } catch (BadCredentialsException e) {
            return unauthorizedBody("Invalid username or password.");
        } catch (Exception e) {
            return unauthorizedBody("Invalid username or password.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return unauthorizedBody(null);
        }
        return ResponseEntity.ok(meBody(auth.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // Invalidate the HTTP session and clear the SecurityContext directly — the
        // Spring Security logout filter is disabled on this filter chain, so we do
        // the same thing it would have done, only without the filter dependency.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private AuthMe meBody(String username) {
        return users.findByUsernameIgnoreCase(username)
                .map(u -> new AuthMe(u.getUsername(), u.getDisplayName(), u.getRole()))
                .orElseGet(() -> new AuthMe(username, username, "LAB_TECH"));
    }

    private ResponseEntity<Map<String, Object>> unauthorizedBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 401);
        body.put("error", "Unauthorized");
        if (message != null) body.put("message", message);
        return ResponseEntity.status(401).body(body);
    }

}
