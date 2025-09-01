package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/admin-panel")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String showAdminPanel(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "admin-panel";
    }

    @GetMapping("/admin/books")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String showAdminBooks(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "admin-panel";
    }
}