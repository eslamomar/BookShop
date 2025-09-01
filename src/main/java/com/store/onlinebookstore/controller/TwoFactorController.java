package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.repository.CustomerRepository;
import com.store.onlinebookstore.security.CustomUserDetailsService;
import com.store.onlinebookstore.service.CustomerService;
import com.store.onlinebookstore.util.SecretKeyUtil;
import com.store.onlinebookstore.util.TotpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;

@Controller
public class TwoFactorController {

    private static final int MAX_2FA_ATTEMPTS = 3;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TotpUtil totpUtil;

    @GetMapping("/2fa")
    public String show2fa(HttpSession session, Model model) {
        // Check if 2FA is actually required
        Boolean required = (Boolean) session.getAttribute("2FA_REQUIRED");
        if (required == null || !required) {
            return "redirect:/login";
        }

        // Check current attempts
        Integer attempts = (Integer) session.getAttribute("2FA_ATTEMPTS");
        if (attempts != null && attempts >= MAX_2FA_ATTEMPTS) {
            session.invalidate();
            return "redirect:/login?error=2fa_locked";
        }

        // Show remaining attempts if there have been failures
        if (attempts != null && attempts > 0) {
            model.addAttribute("remainingAttempts", MAX_2FA_ATTEMPTS - attempts);
            model.addAttribute("showWarning", true);
        }

        return "2fa";
    }

    @PostMapping("/2fa")
    public String verify2fa(@RequestParam String code,
                            HttpServletRequest request,
                            Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login?error=session";
        }

        String email = (String) session.getAttribute("2FA_EMAIL");
        Boolean required = (Boolean) session.getAttribute("2FA_REQUIRED");

        if (email == null || required == null || !required) {
            return "redirect:/login?error=session";
        }

        // Get and increment attempt counter
        Integer attempts = (Integer) session.getAttribute("2FA_ATTEMPTS");
        if (attempts == null) attempts = 0;
        attempts++;
        session.setAttribute("2FA_ATTEMPTS", attempts);

        // Check if max attempts exceeded
        if (attempts >= MAX_2FA_ATTEMPTS) {
            // Lock the account
            customerService.lockAccount(email);
            session.invalidate();
            return "redirect:/login?error=2fa_locked";
        }

        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null) {
            return "redirect:/login?error=user_not_found";
        }

        try {
            SecretKey key = SecretKeyUtil.fromBase32(customer.getSecret());
            if (!totpUtil.verifyCode(code, key)) {
                // Invalid code - show error with remaining attempts
                model.addAttribute("error", "Invalid verification code");
                model.addAttribute("remainingAttempts", MAX_2FA_ATTEMPTS - attempts);
                model.addAttribute("showWarning", true);
                return "2fa";
            }
        } catch (Exception e) {
            // Handle any errors in code verification
            model.addAttribute("error", "Error verifying code");
            model.addAttribute("remainingAttempts", MAX_2FA_ATTEMPTS - attempts);
            return "2fa";
        }

        customerService.resetFailedAttempts(email);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        session.setAttribute("customer", customer);

        session.removeAttribute("2FA_EMAIL");
        session.removeAttribute("2FA_REQUIRED");
        session.removeAttribute("2FA_ATTEMPTS");

        return "redirect:/";
    }
}