package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.model.Role;
import com.store.onlinebookstore.repository.BookRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class AdminController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/admin-panel")
    public String showAdminPanel(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("customer");
        if (customer == null || customer.getRole() != Role.ADMIN) {
            return "redirect:/login";
        }

        model.addAttribute("books", bookRepository.findAll()); // Load books here
        return "admin-panel";
    }
}

