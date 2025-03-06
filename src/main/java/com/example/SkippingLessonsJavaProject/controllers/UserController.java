package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.repositories.UserRepository;
import com.example.SkippingLessonsJavaProject.models.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей получен",
                    content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос (пользователь уже существует, и т.д.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "No Login", value = "{\n  \"message\": \"Вы не зарегистрированы\"\n}"),
                            })),
            @ApiResponse(responseCode = "401", description = "Неверный запрос (вышли из аккаунта)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Logged Out", value = "{\n  \"message\": \"Вы вышли из системы, повторите вход\"\n}"),
                            })),
            @ApiResponse(responseCode = "403", description = "Отсутствие прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Not Admin", value = "{\n  \"message\": \"Запрос отклонен, у вас недостаточно прав, воспользоваться может только АДМИН\"\n}"),
                            })),
            @ApiResponse(responseCode = "404", description = "Объект не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "User Not Found", value = "{\n  \"message\": \"Пользователь не найден\"\n}"),
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}"
                            )))
    })
    public ResponseEntity<?> list (HttpServletRequest request){
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы не зарегистрированы"));
            }

            String token = authHeader.substring(7);

            if (tokenBlackList.isTokenBlackList(token)) {
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы вышли из системы, повторите вход"));
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();


            String login = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(login);
            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не найден"));
            }

            User ouruser = userLogin.get();

            if (!Objects.equals(ouruser.getRole().toString(), "АДМИН")) {
                return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав, воспользоваться может только АДМИН"));
            }

            List<User> userList = userDb.findAll();

            Map<String, List<User>> response = new HashMap<>();
            response.put("users array", userList);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }

    @GetMapping("/role")
    @Operation(summary = "Запрос на получение роли пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Роль пользователя получена",
                    content = @Content(schema = @Schema(implementation = UserRoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос (пользователь уже существует, и т.д.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "No Login", value = "{\n  \"message\": \"Вы не зарегистрированы\"\n}"),
                            })),
            @ApiResponse(responseCode = "401", description = "Неверный запрос (вышли из аккаунта)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Logged Out", value = "{\n  \"message\": \"Вы вышли из системы, повторите вход\"\n}"),
                            })),
            @ApiResponse(responseCode = "404", description = "Объект не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "User Not Found", value = "{\n  \"message\": \"Пользователь не найден\"\n}"),
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}"
                            )))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> role (HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы не зарегистрированы"));
            }

            String token = authHeader.substring(7);

            if (tokenBlackList.isTokenBlackList(token)) {
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы вышли из системы, повторите вход"));
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();


            String login = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(login);
            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не найден"));
            }

            User user = userLogin.get();
            UserRoleResponse response = new UserRoleResponse(
                    user.getRole()
            );

            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }

    @PutMapping("/changeRole")
    @Operation(summary = "Изменение роли пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Роль пользователя успешно изменена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = "{\n  \"message\": \"Роль пользователя успешно изменена\"\n}"
                            ))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос (пользователь уже существует, и т.д.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "No Login", value = "{\n  \"message\": \"Вы не зарегистрированы\"\n}"),
                            })),
            @ApiResponse(responseCode = "401", description = "Неверный запрос (вышли из аккаунта)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Logged Out", value = "{\n  \"message\": \"Вы вышли из системы, повторите вход\"\n}"),
                            })),
            @ApiResponse(responseCode = "403", description = "Отсутствие прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Not Admin", value = "{\n  \"message\": \"Запрос отклонен, у вас недостаточно прав, воспользоваться может только АДМИН\"\n}"),
                            })),
            @ApiResponse(responseCode = "404", description = "Объект не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "User Not Found", value = "{\n  \"message\": \"Пользователь не найден\"\n}"),
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}"
                            )))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> changeRole (HttpServletRequest authRequest,@RequestParam String firstLogin, @RequestParam UserRole firstRole) {
        try {
            String authHeader = authRequest.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы не зарегистрированы"));
            }

            String token = authHeader.substring(7);

            if (tokenBlackList.isTokenBlackList(token)) {
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы вышли из системы, повторите вход"));
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();


            String login = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(login);
            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не найден"));
            }

            User user = userLogin.get();
            String role = user.getRole().toString();

            if(Objects.equals(role, "АДМИН"))
            {
                userLogin = userDb.findByLogin(firstLogin);
                User changedUser = userLogin.get();
                changedUser.setRole(firstRole);
                userDb.save(changedUser);
                return  ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Роль пользователя успешно изменена"));
            }
            else  return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав, воспользоваться может только АДМИН"));
        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }
}
