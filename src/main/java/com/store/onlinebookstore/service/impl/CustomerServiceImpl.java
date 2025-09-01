package com.store.onlinebookstore.service.impl;

import com.store.onlinebookstore.dto.CustomerDto;
import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.model.Role;
import com.store.onlinebookstore.repository.CustomerRepository;
import com.store.onlinebookstore.repository.RoleRepository;
import com.store.onlinebookstore.service.CustomerService;
import org.apache.commons.codec.binary.Base32;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void saveCustomer(CustomerDto customerDto) {
        Customer customer = new Customer();
        customer.setName(customerDto.getFirstName() + " " + customerDto.getLastName());
        customer.setEmail(customerDto.getEmail());
        customer.setDateOfBirth(customerDto.getDateOfBirth());
        customer.setAddress(customerDto.getAddress());
        customer.setPhoneNumber(customerDto.getPhoneNumber());

        // Encrypt the password
        customer.setPassword(passwordEncoder.encode(customerDto.getPassword()));

        // Handle 2FA settings
        customer.setUsing2FA(customerDto.isUsing2FA());
        if (customerDto.isUsing2FA() && customerDto.getSecret() != null) {
            customer.setSecret(customerDto.getSecret());
        }

        // Assign default role
        Role role = roleRepository.findByName("ROLE_CUSTOMER");
        if (role == null) {
            role = checkRoleExist("ROLE_CUSTOMER");
        }
        customer.setRoles(Arrays.asList(role));

        customerRepository.save(customer);
    }

    @Override
    public Customer findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email).orElse(null);
    }

    @Override
    public List<CustomerDto> findAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementFailedAttempts(String email) {
        customerRepository.findByEmail(email).ifPresent(customer -> {
            int newFailAttempts = customer.getLoginAttempts() + 1;
            customer.setLoginAttempts(newFailAttempts);

            if (newFailAttempts >= MAX_FAILED_ATTEMPTS) {
                customer.setAccountLocked(true);
            }

            customerRepository.save(customer);
        });
    }

    @Override
    @Transactional
    public void resetFailedAttempts(String email) {
        customerRepository.findByEmail(email).ifPresent(customer -> {
            customer.setLoginAttempts(0);
            customerRepository.save(customer);
        });
    }

    @Override
    @Transactional
    public void lockAccount(String email) {
        customerRepository.findByEmail(email).ifPresent(customer -> {
            customer.setAccountLocked(true);
            customerRepository.save(customer);
        });
    }

    @Override
    public boolean isAccountLocked(String email) {
        return customerRepository.findByEmail(email)
                .map(Customer::isAccountLocked)
                .orElse(false);
    }

    @Override
    public String generateQRUrl(Customer customer) throws UnsupportedEncodingException {
        if (customer == null || customer.getSecret() == null) {
            throw new IllegalArgumentException("Customer or secret is null");
        }

        String issuer = "BookStore";
        String label = customer.getEmail();

        // Create the otpauth URL
        String otpauth = "otpauth://totp/" +
                URLEncoder.encode(issuer + ":" + label, StandardCharsets.UTF_8) +
                "?secret=" + URLEncoder.encode(customer.getSecret(), StandardCharsets.UTF_8) +
                "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8);

        // Generate QR code URL using QuickChart
        String qrUrl = "https://quickchart.io/qr?size=200&text=" +
                URLEncoder.encode(otpauth, StandardCharsets.UTF_8);

        return qrUrl;
    }

    @Override
    @Transactional
    public CustomerDto updateCustomer2FA(boolean use2FA) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Customer customer = customerRepository.findByEmail(auth.getName()).orElse(null);
        if (customer == null) {
            return null;
        }

        customer.setUsing2FA(use2FA);
        if (use2FA) {
            // Generate new secret for 2FA
            Base32 base32 = new Base32();
            String secret = base32.encodeToString(
                    (customer.getEmail() + System.currentTimeMillis()).getBytes());
            customer.setSecret(secret);
        } else {
            // Clear secret when disabling 2FA
            customer.setSecret(null);
        }

        customerRepository.save(customer);
        return convertEntityToDto(customer);
    }

    private CustomerDto convertEntityToDto(Customer customer) {
        CustomerDto customerDto = new CustomerDto();
        String[] name = customer.getName().split(" ", 2);
        customerDto.setFirstName(name[0]);
        if (name.length > 1) {
            customerDto.setLastName(name[1]);
        }
        customerDto.setEmail(customer.getEmail());
        customerDto.setDateOfBirth(customer.getDateOfBirth());
        customerDto.setAddress(customer.getAddress());
        customerDto.setPhoneNumber(customer.getPhoneNumber());
        customerDto.setUsing2FA(customer.isUsing2FA());
        customerDto.setSecret(customer.getSecret());
        return customerDto;
    }

    private Role checkRoleExist(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }
}