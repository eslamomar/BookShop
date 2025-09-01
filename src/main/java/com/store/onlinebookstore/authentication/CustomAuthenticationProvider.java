package com.store.onlinebookstore.authentication;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.repository.CustomerRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    private final CustomerRepository customerRepository;

    public CustomAuthenticationProvider(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication authResult = super.authenticate(authentication);

        Customer customer = customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails principal = (UserDetails) authResult.getPrincipal();
        return new UsernamePasswordAuthenticationToken(
                principal,
                authResult.getCredentials(),
                principal.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
