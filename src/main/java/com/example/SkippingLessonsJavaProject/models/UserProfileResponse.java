package com.example.SkippingLessonsJavaProject.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private String name;
    private String login;
    private String phone;
    private UserRole role;

}
