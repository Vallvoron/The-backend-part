package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.entitys.SkippingRequest;
import com.example.SkippingLessonsJavaProject.entitys.User;
import com.example.SkippingLessonsJavaProject.models.SkippingRequestRegisterRequest;
import com.example.SkippingLessonsJavaProject.models.TokenBlackList;
import com.example.SkippingLessonsJavaProject.repositories.SkippingRequestRepository;
import com.example.SkippingLessonsJavaProject.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.Optional;

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


            // Создаем новую заявку
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

            // Сохраняем заявку в базу данных
            skippingRequestRepository.save(skippingRequest);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Пропуск успешно создан, ожидайте одобрение администратора"));
    }catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }

    }
}
