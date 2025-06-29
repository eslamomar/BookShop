package com.store.onlinebookstore.controller;

import com.store.onlinebookstore.model.Book;
import com.store.onlinebookstore.model.CartItem;
import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.repository.BookRepository;
import com.store.onlinebookstore.repository.CartItemRepository;
import com.store.onlinebookstore.repository.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartItemRepository cartItemRepo;

    @Autowired
    private CustomerRepository customerRepo;

    @Autowired
    private BookRepository bookRepo;

    private Customer getCurrentCustomer(HttpSession session) {
        return (Customer) session.getAttribute("customer");
    }

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        Customer customer = getCurrentCustomer(session);
        if (customer == null) return "redirect:/login";

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
    public String addToCart(@PathVariable Long bookId, HttpSession session) {
        Customer customer = getCurrentCustomer(session);
        if (customer == null) return "redirect:/login";

        Book book = bookRepo.findById(bookId).orElse(null);
        if (book == null) return "redirect:/?error=BookNotFound";

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
    public String removeFromCart(@PathVariable Long itemId, HttpSession session) {
        Customer customer = getCurrentCustomer(session);
        if (customer == null) return "redirect:/login";

        cartItemRepo.deleteById(itemId);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(HttpSession session, Model model) {
        Customer customer = getCurrentCustomer(session);
        if (customer == null) return "redirect:/login";

        List<CartItem> items = cartItemRepo.findByCustomer(customer);

        double total = items.stream()
                .mapToDouble(item -> item.getBook().getPrice() * item.getQuantity())
                .sum();

        model.addAttribute("cartItems", items);
        model.addAttribute("total", total);
        return "checkout"; // This must match checkout.html
    }


    @PostMapping("/checkout")
    public String processCheckout(HttpSession session,
                                  @RequestParam String cardNumber,
                                  @RequestParam String expiry,
                                  @RequestParam String cvv,
                                  Model model) {

        Customer customer = getCurrentCustomer(session);
        if (customer == null) return "redirect:/login";

        List<CartItem> items = cartItemRepo.findByCustomer(customer);

        // Reduce book copies
        for (CartItem item : items) {
            Book book = item.getBook();
            int currentStock = book.getCopiesAvailable();
            int quantity = item.getQuantity();

            if (currentStock < quantity) {
                model.addAttribute("error", "Not enough stock for " + book.getTitle());
                return "checkout";
            }

            book.setCopiesAvailable(currentStock - quantity);
            bookRepo.save(book);
        }

        cartItemRepo.deleteAll(items); // clear cart after checkout
        model.addAttribute("message", "Payment successful! Thank you for your purchase.");
        return "confirmation";
    }


}

