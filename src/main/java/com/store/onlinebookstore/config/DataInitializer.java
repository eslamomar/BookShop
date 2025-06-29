package com.store.onlinebookstore.config;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.model.Role;
import com.store.onlinebookstore.repository.CustomerRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired
    private CustomerRepository customerRepository;

    @PostConstruct
    public void init() {
        // If admin doesn't exist, create one
        if (customerRepository.findByEmail("admin@store.com").isEmpty()) {
            Customer admin = new Customer();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@store.com");
            admin.setPassword("admin"); // 🔐 Consider encoding this in production
            admin.setAddress("Admin HQ");
            admin.setPhoneNumber("0000000000");
            admin.setRole(Role.ADMIN);
            customerRepository.save(admin);
        }
    }
}
