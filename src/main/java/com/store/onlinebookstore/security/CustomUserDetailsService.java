package com.store.onlinebookstore.security;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.model.Role;
import com.store.onlinebookstore.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (customer.isAccountLocked()) {
            throw new RuntimeException("Account is locked due to multiple failed login attempts");
        }

        return User.builder()
                .username(customer.getEmail())
                .password(customer.getPassword())
                .authorities(mapRolesToAuthorities(customer.getRoles()))
                .accountLocked(customer.isAccountLocked())
                .build();
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(role -> {
                    String roleName = role.getName();
                    // Add ROLE_ prefix if not present
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());
    }
}