package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.UserRepository;
import com.example.SkippingLessonsJavaProject.models.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/user")
@Tag(name = "User")
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private SecretKey key;
    private final UserRepository userDb;
    private final TokenBlackList tokenBlackList;
    public UserController(UserRepository userRepository, TokenBlackList tokenBlackList){
        this.userDb = userRepository;
        this.tokenBlackList = tokenBlackList;
    }

    @GetMapping("/list")
    @Operation(summary = "Запрос на выдачу списка пользователей")
    public ResponseEntity<?> list (HttpServletRequest request){
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).body("Вы не зарегистрированы");
            }

            String token = authHeader.substring(7);

            if (tokenBlackList.isTokenBlackList(token)) {
                return ResponseEntity.status(401).body("Вы вышли из системы, повторите вход");
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();


            String login = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(login);
            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).body("Пользователь не найден");
            }

            List<User> userList = userDb.findAll();

            Map<String, List<User>> response = new HashMap<>();
            response.put("users array", userList);

            return ResponseEntity.ok(response);

        } catch (Exception error) {
            return ResponseEntity.internalServerError().body("Ошибка: " + error.getMessage());
        }
    }

    @GetMapping("/role")
    @Operation(summary = "Запрос на получение роли пользователя")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> role (HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).body("Вы не зарегистрированы");
            }

            String token = authHeader.substring(7);

            if (tokenBlackList.isTokenBlackList(token)) {
                return ResponseEntity.status(401).body("Вы вышли из системы, повторите вход");
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();


            String login = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(login);
            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).body("Пользователь не найден");
            }

            User user = userLogin.get();
            UserRoleResponse response = new UserRoleResponse(
                    user.getRole()
            );

            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.internalServerError().body("Ошибка: " + error.getMessage());
        }
    }

    @PutMapping("/changeRole")
    @Operation(summary = "Изменение роли пользователя")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> changeRole (HttpServletRequest authRequest,@Valid @RequestBody UserChangeRoleRequest request) {
        try {
            String authHeader = authRequest.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).body("Вы не зарегистрированы");
            }

            String token = authHeader.substring(7);

            if (tokenBlackList.isTokenBlackList(token)) {
                return ResponseEntity.status(401).body("Вы вышли из системы, повторите вход");
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();


            String login = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(login);
            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).body("Пользователь не найден");
            }

            User user = userLogin.get();
            String role = user.getRole().toString();

            if(Objects.equals(role, "АДМИН")||Objects.equals(role, "ДЕКАНАТ"))
            {
                userLogin = userDb.findByLogin(request.getLogin());
                User changedUser = userLogin.get();
                changedUser.setRole(request.getNewRole());
                userDb.save(changedUser);
                return  ResponseEntity.ok("Роль пользователя успешно изменена.");
            }
            else  return ResponseEntity.badRequest().body("Запрос отклонен: недостаточные права");
        } catch (Exception error) {
            logger.error("Error occurred while changing role: ", error);
            return ResponseEntity.internalServerError().body("Ошибка: " + error.getMessage());
        }
    }
}
