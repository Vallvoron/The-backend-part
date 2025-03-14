package com.example.SkippingLessonsJavaProject.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private UUID Id;
    private String name;
    private String login;
    private String phone;
    private UserRole role;

}
