package com.example.SkippingLessonsJavaProject.configs;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class SecretKeyGenerator {

    private SecretKey key;

    @Bean
    public SecretKey secretKey() {

        key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Или любой другой алгоритм
        return key;
    }
}
