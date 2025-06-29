package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.model.Author;
import com.store.onlinebookstore.model.Book;
import com.store.onlinebookstore.repository.AuthorRepository;
import com.store.onlinebookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;


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

    @GetMapping("/books/add")
    public String showAddForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("authorsInput", "");  // empty input for new book
        return "book-form";
    }

    @GetMapping("/books/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Book> bookOpt = bookRepository.findById(id);
        if (bookOpt.isEmpty()) {
            return "redirect:/admin/books";
        }
        Book book = bookOpt.get();
        model.addAttribute("book", book);

        // Create comma separated author names string
        String authors = book.getAuthors()
                .stream()
                .map(Author::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        model.addAttribute("authorsInput", authors);

        return "book-form";
    }


    // Save new or updated book
    @PostMapping("/books/save")
    public String saveBook(@ModelAttribute Book book,
                           @RequestParam("authorsInput") String authorsInput,
                           Model model) {
        if (book.getId() == null) {
            Optional<Book> existingBook = bookRepository.findByIsbn(book.getIsbn());
            if (existingBook.isPresent()) {
                model.addAttribute("error", "Book with this ISBN already exists.");
                return "book-form";
            }
        }

        List<Author> authors = Arrays.stream(authorsInput.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> authorRepository.findByName(name).orElseGet(() -> {
                    Author author = new Author();
                    author.setName(name);
                    return authorRepository.save(author);
                }))
                .toList();

        book.setAuthors(authors);
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
