package com.store.onlinebookstore.security;

import com.store.onlinebookstore.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private CustomerService customerService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("username");

        if (email != null && !email.isEmpty()) {
            // Check if account is already locked
            if (customerService.isAccountLocked(email)) {
                getRedirectStrategy().sendRedirect(request, response,
                        "/login?error=locked");
                return;
            }

            // Increment failed attempts
            customerService.incrementFailedAttempts(email);

            // Check if account should be locked after this attempt
            if (customerService.isAccountLocked(email)) {
                getRedirectStrategy().sendRedirect(request, response,
                        "/login?error=locked");
                return;
            }
        }

        // Default behavior for other failures
        super.setDefaultFailureUrl("/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}