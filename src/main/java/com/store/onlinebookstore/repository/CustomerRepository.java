package com.store.onlinebookstore.repository;
import com.store.onlinebookstore.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    //Customer findByEmail(String email);
    //Optional<Customer> findByUsername(String username);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByAccountLocked(boolean locked);



}
