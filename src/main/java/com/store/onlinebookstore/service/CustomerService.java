package com.store.onlinebookstore.service;

import com.store.onlinebookstore.dto.CustomerDto;
import com.store.onlinebookstore.model.Customer;

import java.io.UnsupportedEncodingException;
import java.util.List;

public interface CustomerService {
    void saveCustomer(CustomerDto customerDto);

    Customer findCustomerByEmail(String email);

    List<CustomerDto> findAllCustomers();

    void incrementFailedAttempts(String email);

    void resetFailedAttempts(String email);

    void lockAccount(String email);

    boolean isAccountLocked(String email);

    String generateQRUrl(Customer customer) throws UnsupportedEncodingException;
    CustomerDto updateCustomer2FA(boolean use2FA);
}