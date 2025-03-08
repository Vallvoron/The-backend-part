package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.entitys.User;
import com.example.SkippingLessonsJavaProject.repositories.UserRepository;
import com.example.SkippingLessonsJavaProject.models.*;
import com.example.SkippingLessonsJavaProject.repositories.UsersForRegisterRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


import jakarta.validation.Valid;

import javax.crypto.SecretKey;


@RestController
@RequestMapping("/api/account")
@Tag(name = "Account")
@Validated
public class AccountController {

    @Autowired
    private SecretKey key;

    private final UsersForRegisterRepository usersForRegisterDb;
    private final UserRepository userDb;
    private final TokenBlackList tokenBlackList;
    public AccountController(UsersForRegisterRepository usersForRegisterDb, UserRepository userRepository, TokenBlackList tokenBlackList){
        this.userDb = userRepository;
        this.tokenBlackList = tokenBlackList;
        this.usersForRegisterDb = usersForRegisterDb;
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная регистрация. Заявка отправлена.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = "{\n  \"message\": \"Заявка успешно отправлена, ожидайте одобрение администратора\"\n}"
                            ))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос (пользователь уже существует, и т.д.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "User Exists", value = "{\n  \"message\": \"Пользователь с таким email уже существует\"\n}"),
                                    @ExampleObject(name = "Application Pending", value = "{\n  \"message\": \"Заявка с таким email уже отправлена\"\n}")
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}"
                            )))
    })
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterRequest request){
        try {
            if (userDb.findByLogin(request.getLogin()).isPresent()){
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь с таким email уже существует"));
            }

            if (usersForRegisterDb.findByLogin(request.getLogin()).isPresent()){
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Заявка с таким email уже отправлена"));
            }

            UsersForRegister newUser = new UsersForRegister();
            newUser.setLogin(request.getLogin());
            newUser.setPassword(request.getPassword());
            newUser.setName(request.getName());
            newUser.setPhone(request.getPhone());
            newUser.setRole(request.getRole() != null ? request.getRole() : UserRole.ПОЛЬЗОВАТЕЛЬ);

            usersForRegisterDb.save(newUser);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Заявка успешно отправлена, ожидайте одобрение администратора"));

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }

    }


    @PostMapping("/approve")
    @Operation(summary = "Одобрение заявок регистрации пользователей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка одобрена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = "{\n  \"message\": \"Пользователь login добавлен в систему\"\n}"
                            ))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос (пользователь уже существует, и т.д.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "No Login", value = "{\n  \"message\": \"Вы не зарегистрированы\"\n}"),
                                    @ExampleObject(name = "User Exists", value = "{\n  \"message\": \"Пользователь уже зарегистрирован\"\n}"),
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
                                    @ExampleObject(name = "Application Not Found", value = "{\n  \"message\": \"Заявка на регистрацию не найдена\"\n}"),
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}"
                            )))
    })
    @Transactional
    public ResponseEntity<?> approveUser(HttpServletRequest request, @RequestParam String login, @RequestParam UserRole role) {
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

            String ourlogin = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(ourlogin);

            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не найден"));
            }

            User user = userLogin.get();

            if (!Objects.equals(user.getRole().toString(), "АДМИН")) {
                return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав, воспользоваться может только АДМИН"));
            }

            Optional<UsersForRegister> optionalUser = usersForRegisterDb.findByLogin(login);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Заявка на регистрацию не найдена"));
            }

            UsersForRegister usersForRegister = optionalUser.get();

            if (userDb.findByLogin(login).isPresent()) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь уже зарегистрирован"));
            }

            User newUser = new User();
            newUser.setLogin(usersForRegister.getLogin());
            newUser.setPassword(usersForRegister.getPassword());
            newUser.setName(usersForRegister.getName());
            newUser.setPhone(usersForRegister.getPhone());

            newUser.setRole(role);

            userDb.save(newUser);

            usersForRegisterDb.delete(usersForRegister);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь " + login + " добавлен в систему"));

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }


    @GetMapping("/registerList")
    @Operation(summary = "Список заявок на регистрацию")
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
    public ResponseEntity<?> registerList (HttpServletRequest request){
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

            String ourlogin = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(ourlogin);

            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не наден"));
            }

            User user = userLogin.get();

            if (!Objects.equals(user.getRole().toString(), "АДМИН")) {
                return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав, воспользоваться может только АДМИН"));
            }

            List<UsersForRegister> users = usersForRegisterDb.findAll();

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("users", users));

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }



    @PostMapping("/login")
    @Operation(summary = "Авторизация пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен входа получен",
                    content = @Content(examples = @ExampleObject(
                            name = "Success",
                            value = "{\n  \"token\": \"JWTToken\"\n}"
                    ))),
            @ApiResponse(responseCode = "401", description = "Неверный запрос (неправильные данные)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Wrong Login", value = "{\n  \"message\": \"Неверный email или password\"\n}"),
                                    @ExampleObject(name = "Wrong Password", value = "{\n  \"message\": \"Неверный password\"\n}"),
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}"
                            )))
    })
    public ResponseEntity<?> login (@Valid @RequestBody UserSingInRequest request){
        try{

            Optional<User> dataUser = userDb.findByLogin(request.getLogin());

            if (dataUser.isEmpty()){
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Неверный email или password"));
            }

            User user = dataUser.get();

            if (!user.getPassword().equals(request.getPassword())){
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Неверный password"));
            }

            String token = generateToken(user);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }



    @GetMapping("/profile")
    @Operation(summary = "Профиль пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль пользователя получен",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
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
    public ResponseEntity<?> profile(HttpServletRequest request) {
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
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не наден"));
            }

            User user = userLogin.get();
            UserProfileResponse response = new UserProfileResponse(
                    user.getName(),
                    user.getLogin(),
                    user.getPhone(),
                    user.getRole()
            );

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);
        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }


    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Выход из системы",
                    content = @Content(examples = @ExampleObject(
                            name = "Success",
                            value = "{\n  \"message\": \"Вы успешно вышли из системы\"\n}"
                    ))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос (пользователь уже существует, и т.д.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "No Login", value = "{\n  \"message\": \"Токен не найден или вы еще не зарегистрированы\"\n}"),
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}"
                            )))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> logout (HttpServletRequest request){
        try{
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")){
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Токен не найден или вы еще не зарегистрированы"));
            }

            String token = authHeader.substring(7);
            tokenBlackList.addToken(token);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы успешно вышли из системы"));
        }catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }

    @GetMapping("/checkToken")
    @Operation(summary = "Проверка токена пользователя")
    public ResponseEntity<?> checkToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            return ResponseEntity.ok().body("");
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(401).body("");
        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
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
