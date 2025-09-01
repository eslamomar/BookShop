package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.dto.CustomerDto;
import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.model.Role;
import com.store.onlinebookstore.repository.BookRepository;
import com.store.onlinebookstore.repository.CustomerRepository;
import com.store.onlinebookstore.repository.RoleRepository;
import com.store.onlinebookstore.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CustomerService customerService;

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "success", required = false) String success,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username, password, or verification code");
        }
        if (logout != null) {
            model.addAttribute("success", "You have been logged out successfully");
        }
        if (success != null) {
            model.addAttribute("success", "Registration successful! Please login.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        CustomerDto customer = new CustomerDto();
        model.addAttribute("customer", customer);
        return "register";
    }

    @PostMapping("/register/save")
    public String registration(@Valid @ModelAttribute("customer") CustomerDto customerDto,
                               BindingResult result,
                               Model model) {

        // Check if customer already exists
        Customer existingCustomer = customerRepository.findByEmail(customerDto.getEmail()).orElse(null);
        if (existingCustomer != null) {
            result.rejectValue("email", "error.customer",
                    "There is already an account registered with that email");
        }

        if (result.hasErrors()) {
            model.addAttribute("customer", customerDto);
            return "register";
        }

        // Create new customer
        Customer customer = new Customer();
        customer.setName(customerDto.getFirstName() + " " + customerDto.getLastName());
        customer.setEmail(customerDto.getEmail());
        customer.setPassword(passwordEncoder.encode(customerDto.getPassword()));
        customer.setDateOfBirth(customerDto.getDateOfBirth());
        customer.setAddress(customerDto.getAddress());
        customer.setPhoneNumber(customerDto.getPhoneNumber());

        // Debug logging
        System.out.println("2FA Enabled from form: " + customerDto.isUsing2FA());

        // Handle 2FA if enabled
        if (customerDto.isUsing2FA()) {
            customer.setUsing2FA(true);
            Base32 base32 = new Base32();
            // Generate a proper random secret
            byte[] bytes = new byte[20];
            new java.security.SecureRandom().nextBytes(bytes);
            String secret = base32.encodeToString(bytes);
            customer.setSecret(secret);

            System.out.println("Generated secret: " + secret);
        } else {
            customer.setUsing2FA(false);
        }

        // Assign default role
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER");
        if (customerRole == null) {
            customerRole = new Role("ROLE_CUSTOMER");
            roleRepository.save(customerRole);
        }
        customer.setRoles(Arrays.asList(customerRole));

        // Save the customer
        Customer savedCustomer = customerRepository.save(customer);

        // If 2FA is enabled, show QR code
        if (savedCustomer.isUsing2FA()) {
            try {
                String qrUrl = customerService.generateQRUrl(savedCustomer);
                System.out.println("Generated QR URL: " + qrUrl);
                model.addAttribute("qr", qrUrl);
                return "qrcode";  // This should show the QR code page
            } catch (Exception e) {
                e.printStackTrace();
                // If QR generation fails, still redirect to login
                return "redirect:/login?success";
            }
        }

        return "redirect:/login?success";
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // Add books that index.html requires
        model.addAttribute("books", bookRepository.findAll());

        // Set customer in session for the navigation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Customer customer = customerRepository.findByEmail(auth.getName()).orElse(null);
            if (customer != null) {
                session.setAttribute("customer", customer);
            }
        }

        return "index";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    // New endpoint for managing 2FA settings
    @GetMapping("/account/2fa")
    public String show2FASettings(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Customer customer = customerRepository.findByEmail(auth.getName()).orElse(null);
        if (customer != null) {
            model.addAttribute("is2FAEnabled", customer.isUsing2FA());
        }
        return "2fa-settings";
    }

    @PostMapping("/account/update/2fa")
    @ResponseBody
    public Map<String, String> update2FA(@RequestParam("use2FA") boolean use2FA) {
        try {
            CustomerDto updatedCustomer = customerService.updateCustomer2FA(use2FA);
            if (use2FA && updatedCustomer != null) {
                Customer customer = customerRepository.findByEmail(updatedCustomer.getEmail()).orElse(null);
                if (customer != null) {
                    String qrUrl = customerService.generateQRUrl(customer);
                    return Map.of("status", "success", "qrUrl", qrUrl);
                }
            }
            return Map.of("status", "success", "message", "2FA disabled");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}