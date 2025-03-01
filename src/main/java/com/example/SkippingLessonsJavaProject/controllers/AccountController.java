package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.UserRepository;
import com.example.SkippingLessonsJavaProject.configs.SecretKeyGenerator;
import com.example.SkippingLessonsJavaProject.models.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import jakarta.validation.Valid;

import javax.crypto.SecretKey;


@RestController
@RequestMapping("/api/account")
@Tag(name = "Account")
@Validated
public class AccountController {

    @Autowired
    private SecretKey key;

    private final UserRepository userDb;
    private final TokenBlackList tokenBlackList;
    public AccountController(UserRepository userRepository, TokenBlackList tokenBlackList){
        this.userDb = userRepository;
        this.tokenBlackList = tokenBlackList;
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация пользователя")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterRequest request){
        try {
            Optional<User> existingUser = userDb.findByLogin(request.getLogin());
            if (existingUser.isPresent()){
                return ResponseEntity.badRequest().body("Пользователь с таким email уже существует");
            }

            User newUser = new User();
            newUser.setLogin(request.getLogin());
            newUser.setPassword(request.getPassword());
            newUser.setName(request.getName());
            newUser.setPhone(request.getPhone());
            newUser.setRole(request.getRole() != null ? request.getRole() : UserRole.ПОЛЬЗОВАТЕЛЬ);

            userDb.save(newUser);

            String token = generateToken(newUser);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);

            return ResponseEntity.ok(response);

        } catch (Exception error) {
            return ResponseEntity.internalServerError().body("Ошибка: " + error.getMessage());
        }



    }


    @PostMapping("/login")
    @Operation(summary = "Авторизация пользователя")
    public ResponseEntity<?> login (@Valid @RequestBody UserSingInRequest request){
        try{

            Optional<User> dataUser = userDb.findByLogin(request.getLogin());

            if (dataUser.isEmpty()){
                return ResponseEntity.status(401).body("Неверный email или password");
            }

            User user = dataUser.get();

            if (!user.getPassword().equals(request.getPassword())){
                return ResponseEntity.status(401).body("Неверный password");
            }

            String token = generateToken(user);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);

            return ResponseEntity.ok(response);

        } catch (Exception error) {
            return ResponseEntity.internalServerError().body("Ошибка: " + error.getMessage());
        }
    }



    @GetMapping("/profile")
    @Operation(summary = "Профиль пользователя")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> profile(HttpServletRequest request) {
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
            UserProfileResponse response = new UserProfileResponse(
                    user.getName(),
                    user.getLogin(),
                    user.getPhone(),
                    user.getRole()
            );

            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.internalServerError().body("Ошибка: " + error.getMessage());
        }
    }


    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> logout (HttpServletRequest request){
        try{
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")){
                return ResponseEntity.badRequest().body("Токен не найден или вы еще не зарегистрированы");
            }

            String token = authHeader.substring(7);
            tokenBlackList.addToken(token);

            return ResponseEntity.ok("Вы успешно вышли из системы");
        }catch (Exception error) {
            return ResponseEntity.internalServerError().body("Ошибка: " + error.getMessage());
        }
    }


    private String generateToken(User user){
        return Jwts.builder()
                .setSubject(user.getLogin())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
