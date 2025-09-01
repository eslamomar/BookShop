package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.model.Book;
import com.store.onlinebookstore.model.CartItem;
import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.repository.BookRepository;
import com.store.onlinebookstore.repository.CartItemRepository;
import com.store.onlinebookstore.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartItemRepository cartItemRepo;

    @Autowired
    private CustomerRepository customerRepo;

    @Autowired
    private BookRepository bookRepo;

    private Customer getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        return customerRepo.findByEmail(email).orElse(null);
    }

    @GetMapping
    public String viewCart(Model model) {
        Customer customer = getCurrentCustomer();
        if (customer == null) {
            return "redirect:/login";
        }

        List<CartItem> items = cartItemRepo.findByCustomer(customer);

        double total = items.stream()
                .filter(item -> item.getBook() != null)
                .mapToDouble(item -> item.getBook().getPrice() * item.getQuantity())
                .sum();

        model.addAttribute("cartItems", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/add/{bookId}")
    public String addToCart(@PathVariable Long bookId) {
        Customer customer = getCurrentCustomer();
        if (customer == null) {
            return "redirect:/login";
        }

        Optional<Book> bookOpt = bookRepo.findById(bookId);
        if (!bookOpt.isPresent()) {
            return "redirect:/?error=BookNotFound";
        }

        Book book = bookOpt.get();
        CartItem item = cartItemRepo.findByCustomerAndBook(customer, book)
                .orElse(new CartItem());

        if (item.getId() == null) {
            item.setCustomer(customer);
            item.setBook(book);
            item.setQuantity(1);
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }

        cartItemRepo.save(item);
        return "redirect:/";
    }

    @GetMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId) {
        Customer customer = getCurrentCustomer();
        if (customer == null) {
            return "redirect:/login";
        }

        // Verify the item belongs to the current customer for security
        Optional<CartItem> itemOpt = cartItemRepo.findById(itemId);
        if (itemOpt.isPresent() && itemOpt.get().getCustomer().getId().equals(customer.getId())) {
            cartItemRepo.deleteById(itemId);
        }
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(Model model) {
        Customer customer = getCurrentCustomer();
        if (customer == null) {
            return "redirect:/login";
        }

        List<CartItem> items = cartItemRepo.findByCustomer(customer);

        if (items.isEmpty()) {
            model.addAttribute("error", "Your cart is empty");
            return "cart";
        }

        double total = items.stream()
                .mapToDouble(item -> item.getBook().getPrice() * item.getQuantity())
                .sum();

        model.addAttribute("cartItems", items);
        model.addAttribute("total", total);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String cardNumber,
                                  @RequestParam String expiry,
                                  @RequestParam String cvv,
                                  Model model) {

        Customer customer = getCurrentCustomer();
        if (customer == null) {
            return "redirect:/login";
        }

        // Basic input validation
        if (cardNumber == null || cardNumber.trim().isEmpty() ||
                expiry == null || expiry.trim().isEmpty() ||
                cvv == null || cvv.trim().isEmpty()) {
            model.addAttribute("error", "Please fill in all payment details");
            return "checkout";
        }

        List<CartItem> items = cartItemRepo.findByCustomer(customer);

        if (items.isEmpty()) {
            model.addAttribute("error", "Your cart is empty");
            return "redirect:/cart";
        }

        // Check stock availability and reduce inventory
        for (CartItem item : items) {
            Book book = item.getBook();
            int currentStock = book.getCopiesAvailable();
            int quantity = item.getQuantity();

            if (currentStock < quantity) {
                model.addAttribute("error", "Not enough stock for " + book.getTitle());
                model.addAttribute("cartItems", items);
                model.addAttribute("total", items.stream()
                        .mapToDouble(cartItem -> cartItem.getBook().getPrice() * cartItem.getQuantity())
                        .sum());
                return "checkout";
            }

            book.setCopiesAvailable(currentStock - quantity);
            bookRepo.save(book);
        }

        // Clear cart after successful checkout
        cartItemRepo.deleteAll(items);

        model.addAttribute("message", "Payment successful! Thank you for your purchase.");
        return "confirmation";
    }
}