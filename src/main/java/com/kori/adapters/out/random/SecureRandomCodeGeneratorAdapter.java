package com.kori.adapters.out.random;

import com.kori.application.port.out.CodeGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomCodeGeneratorAdapter implements CodeGeneratorPort {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String next6Digits() {
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
