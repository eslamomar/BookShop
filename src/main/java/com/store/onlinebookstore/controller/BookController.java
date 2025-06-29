package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.model.Book;
import com.store.onlinebookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/")
    public String viewBooks(Model model) {
        List<Book> books = bookRepository.findAll();
        model.addAttribute("books", books);
        return "index";  // customer view
    }

    @GetMapping("/admin/books")
    public String showAdminPanel(Model model) {
        List<Book> books = bookRepository.findAll();
        model.addAttribute("books", books);
        return "admin-panel"; // admin panel view
    }

    // Show Add Form
    @GetMapping("/books/add")
    public String showAddForm(Model model) {
        model.addAttribute("book", new Book());
        return "book-form";
    }

    // Show Edit Form
    @GetMapping("/books/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Book> bookOpt = bookRepository.findById(id);
        if (bookOpt.isEmpty()) {
            return "redirect:/admin/books";
        }
        model.addAttribute("book", bookOpt.get());
        return "book-form";
    }

    // Save new or updated book
    @PostMapping("/books/save")
    public String saveBook(@ModelAttribute Book book, Model model) {
        // Check for ISBN conflict when adding new book
        if (book.getId() == null) {
            Optional<Book> existingBook = bookRepository.findByIsbn(book.getIsbn());
            if (existingBook.isPresent()) {
                model.addAttribute("error", "Book with this ISBN already exists.");
                return "book-form";
            }
        }
        bookRepository.save(book);
        return "redirect:/admin/books";
    }

    // Delete book
    @GetMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
        }
        return "redirect:/admin/books";
    }
}
