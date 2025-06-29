package com.store.onlinebookstore.repository;

import com.store.onlinebookstore.model.Book;
import com.store.onlinebookstore.model.CartItem;
import com.store.onlinebookstore.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCustomer(Customer customer);
    Optional<CartItem> findByCustomerAndBook(Customer customer, Book book);
}

