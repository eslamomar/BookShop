package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.model.Role;
import com.store.onlinebookstore.repository.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(username);
        if (customerOpt.isPresent() && customerOpt.get().getPassword().equals(password)) {
            Customer customer = customerOpt.get();
            session.setAttribute("customer", customer);

            // Redirect based on role
            if (customer.getRole() == Role.ADMIN) {
                return "redirect:/admin-panel";
            } else {
                return "redirect:/";
            }
        }

        model.addAttribute("error", "Invalid credentials");
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute Customer customer,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", customer);
            return "register";
        }

        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            model.addAttribute("error", "Email already registered");
            return "register";
        }

        customer.setRole(Role.CUSTOMER); // Set default role
        customerRepository.save(customer);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
