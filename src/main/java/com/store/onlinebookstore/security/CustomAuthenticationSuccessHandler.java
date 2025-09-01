package com.store.onlinebookstore.security;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.repository.CustomerRepository;
import com.store.onlinebookstore.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email).orElse(null);

        if (customer != null) {
            // Reset failed attempts on successful login
            customerService.resetFailedAttempts(email);

            HttpSession session = request.getSession(true);

            if (customer.isUsing2FA()) {
                // Store info for 2FA
                session.setAttribute("2FA_REQUIRED", true);
                session.setAttribute("2FA_EMAIL", email);
                session.setAttribute("2FA_ATTEMPTS", 0); // Initialize 2FA attempts counter

                // Clear context so user isn't fully logged in yet
                SecurityContextHolder.clearContext();

                // Redirect to 2FA page
                response.sendRedirect(request.getContextPath() + "/2fa");
                return;
            } else {
                // Set customer in session for non-2FA users
                session.setAttribute("customer", customer);
            }
        }

        // Normal login redirect
        setDefaultTargetUrl("/");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}