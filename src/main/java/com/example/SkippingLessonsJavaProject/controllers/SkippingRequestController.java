package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.entitys.SkippingRequest;
import com.example.SkippingLessonsJavaProject.entitys.User;
import com.example.SkippingLessonsJavaProject.models.*;
import com.example.SkippingLessonsJavaProject.repositories.SkippingRequestRepository;
import com.example.SkippingLessonsJavaProject.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.*;

@RestController
@RequestMapping("/api/skipping-requests")
public class SkippingRequestController {

    @Autowired
    private SecretKey key;

    private final SkippingRequestRepository skippingRequestRepository;
    private final UserRepository userDb;
    private final TokenBlackList tokenBlackList;

    @Autowired
    public SkippingRequestController(SkippingRequestRepository skippingRequestRepository, UserRepository userRepository, TokenBlackList tokenBlackList) {
        this.skippingRequestRepository = skippingRequestRepository;
        this.userDb = userRepository;
        this.tokenBlackList = tokenBlackList;
    }

    @PostMapping("/create")
    @Operation(
            summary = "Создание пропусков",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SkippingRequestRegisterRequest.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> createSkippingRequest(HttpServletRequest authRequest, @Valid @RequestBody SkippingRequestRegisterRequest request) {

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

            String ourlogin = claims.getSubject();
            Optional<User> userLogin = userDb.findByLogin(ourlogin);

            if (userLogin.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не наден"));
            }

            User user = userLogin.get();

            if (!Objects.equals(user.getRole().toString(), "СТУДЕНТ")) {
                return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, пропуска могут создавать только пользователи с ролью СТУДЕНТ"));
            }


            SkippingRequest skippingRequest = new SkippingRequest();
            if(request.getLessons() != null && !request.getLessons().isEmpty())
            {
                if (request.getStartDate().equals(request.getEndDate())){
                    skippingRequest.setStudent(user);
                    skippingRequest.setStartDate(request.getStartDate());
                    skippingRequest.setEndDate(request.getEndDate());
                    skippingRequest.setLessons(request.getLessons());
                    skippingRequest.setReason(request.getReason());
                }
                else return ResponseEntity.badRequest().body("При выборе пар пропуск оформляется только на один день");
            }
            else{
                skippingRequest.setStudent(user);
                skippingRequest.setStartDate(request.getStartDate());
                skippingRequest.setEndDate(request.getEndDate());
                skippingRequest.setReason(request.getReason());
            }

            skippingRequestRepository.save(skippingRequest);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Пропуск успешно создан, ожидайте одобрение администратора"));
        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }

    }


    @GetMapping("/skippingRequestList")
    @Operation(
            summary = "Список пропусков",
            description = "Доступно только для админов",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SkippingRequest.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> skippingRequestList (HttpServletRequest request){
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
            UserRole role = user.getRole();

            List<SkippingRequest> finalList;

            switch (role){
                case ПОЛЬЗОВАТЕЛЬ:
                    return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав"));

                case АДМИН:
                    finalList = skippingRequestRepository.findAll();
                    break;

                case ДЕКАНАТ:
                    finalList = skippingRequestRepository.findAll();
                    break;

                case ПРЕПОДАВАТЕЛЬ:
                    finalList = skippingRequestRepository.findByStatus(SkippingRequestStatus.APPROVED);
                    break;

                case СТУДЕНТ:
                    finalList = skippingRequestRepository.findByStudent(user);
                    break;

                default:
                    return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав"));
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("list", finalList));

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }


    @PutMapping("/changeStatus")
    @Operation(
            summary = "Список пропусков",
            description = "Доступно только для админов и деканата",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Статус пропуска успешно обновлён\"\n}"))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> changeStatus(HttpServletRequest request, @RequestParam UUID skippingRequestId, @RequestParam SkippingRequestStatus newStatus){
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
                if (!Objects.equals(user.getRole().toString(), "ДЕКАНАТ")) {
                    return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав, воспользоваться может только АДМИН или ДЕКАНАТ"));
                }
            }

            Optional<SkippingRequest> skippingRequestOpt = skippingRequestRepository.findById(skippingRequestId);
            if (skippingRequestOpt.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пропуск не найден"));
            }
            SkippingRequest skippingRequest = skippingRequestOpt.get();

            skippingRequest.setStatus(newStatus);
            skippingRequestRepository.save(skippingRequest);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("newStatus", newStatus));

        }catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }


}
