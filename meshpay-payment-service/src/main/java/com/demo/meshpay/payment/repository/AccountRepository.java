package com.demo.meshpay.payment.repository;

import com.demo.meshpay.payment.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByVpa(String vpa);
}
