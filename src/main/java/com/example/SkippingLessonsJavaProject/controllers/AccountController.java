package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.UserRepository;
import com.example.SkippingLessonsJavaProject.models.User;
import com.example.SkippingLessonsJavaProject.models.UserRegisterRequest;
import com.example.SkippingLessonsJavaProject.models.UserRole;
import com.example.SkippingLessonsJavaProject.models.UserSingInRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/account")
@Tag(name = "Account")
@Validated
public class AccountController {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final UserRepository userDb;
    public AccountController(UserRepository userRepository){
        this.userDb = userRepository;
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
