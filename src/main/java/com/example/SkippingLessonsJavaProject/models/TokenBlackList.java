package com.example.SkippingLessonsJavaProject.models;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TokenBlackList {
    private final Set<String> blackList = new HashSet<>();

    public void addToken(String token){
        blackList.add(token);
    }

    public boolean isTokenBlackList(String token){
        return blackList.contains(token);
    }
}
