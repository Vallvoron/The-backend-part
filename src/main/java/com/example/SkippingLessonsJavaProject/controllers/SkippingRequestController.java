package com.example.SkippingLessonsJavaProject.controllers;

import com.example.SkippingLessonsJavaProject.entitys.Confirmation;
import com.example.SkippingLessonsJavaProject.entitys.SkippingRequest;
import com.example.SkippingLessonsJavaProject.entitys.User;
import com.example.SkippingLessonsJavaProject.models.*;
import com.example.SkippingLessonsJavaProject.repositories.ConfirmationRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/skipping-requests")
public class SkippingRequestController {

    @Autowired
    private SecretKey key;

    private final SkippingRequestRepository skippingRequestRepository;
    private final ConfirmationRepository confirmationRepository;
    private final UserRepository userDb;
    private final TokenBlackList tokenBlackList;
    @Value("${file.upload.directory}")
    private String uploadDirectory;

    @Autowired
    public SkippingRequestController(SkippingRequestRepository skippingRequestRepository, ConfirmationRepository confirmationRepository, UserRepository userRepository, TokenBlackList tokenBlackList) {
        this.skippingRequestRepository = skippingRequestRepository;
        this.confirmationRepository = confirmationRepository;
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
    public ResponseEntity<?> skippingRequestList (
            HttpServletRequest request,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) SortSettings sortSetting,
            @RequestParam(required = false) Integer lessonNumber){
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

            if (studentId != null){
                finalList = finalList.stream()
                        .filter(stud -> stud.getStudent().getId().equals(studentId))
                        .collect(Collectors.toList());
            }

            if (startDate != null) {
                finalList = finalList.stream()
                        .filter(time -> time.getStartDate() != null && !time.getStartDate().isBefore(startDate))
                        .collect(Collectors.toList());
            }

            if (endDate != null) {
                finalList = finalList.stream()
                        .filter(time -> time.getEndDate() != null && !time.getEndDate().isAfter(endDate))
                        .collect(Collectors.toList());
            }


            if (lessonNumber != null){
                finalList = finalList.stream()
                        .filter(num -> num.getLessons() != null && num.getLessons().contains(lessonNumber))
                        .collect(Collectors.toList());
            }

            if (sortSetting != null){
                if (sortSetting == SortSettings.ASC){
                    finalList.sort(Comparator.comparing(SkippingRequest::getStartDate));
                } else {
                    finalList.sort(Comparator.comparing(SkippingRequest::getStartDate).reversed());
                }
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("list", finalList));

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }


    @PutMapping("/changeStatus")
    @Operation(
            summary = "Смена статуса у пропуска",
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

            if (newStatus == SkippingRequestStatus.APPROVED) {
                skippingRequest.setApprover(user);
            }

            skippingRequestRepository.save(skippingRequest);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("newStatus", newStatus));

        }catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }





    @PostMapping(value = "/addDocument", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @Operation(
            summary = "Добавление документов к пропуску",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SkippingRequestRegisterRequest.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> createConfirmation(HttpServletRequest authRequest, @RequestPart("request") String request, @RequestPart("files") List<MultipartFile> files){


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

            for(MultipartFile file : files) {
                String filename = file.getOriginalFilename();
                String filePath = uploadDirectory + "/" + UUID.randomUUID() + "_" + filename;
                System.out.println("Сохраняем файл по пути: " + filePath);

                Path path = Paths.get(filePath);
                Files.write(path, file.getBytes());
                Optional<SkippingRequest> skippingRequestOpt = skippingRequestRepository.findById(UUID.fromString(request));
                SkippingRequest skippingRequest = skippingRequestOpt.get();

                Confirmation confirmation = new Confirmation();
                confirmation.setFilename(filename);
                confirmation.setFilePath(filePath);
                confirmation.setSkippingRequest(skippingRequest);
                confirmationRepository.save(confirmation);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Документ(-ы) успешно прикреплен(-ы)"));
        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }

    @GetMapping("/getDocument")
    @Operation(
            summary = "Получение листа документов",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SkippingRequestRegisterRequest.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> getConfirmationList(HttpServletRequest request,  @RequestParam String skippingRequestId) {

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

            Optional<SkippingRequest> skippingRequestOpt = skippingRequestRepository.findById(UUID.fromString(skippingRequestId));
            SkippingRequest pass= skippingRequestOpt.get();
            List<Confirmation> confirmations= confirmationRepository.findBySkippingRequest(pass);
            List<String> filePaths = new ArrayList<>();

            for (Confirmation confirmation : confirmations) {
                String filePath = confirmation.getFilePath();
                java.io.File file = new java.io.File(filePath);

                if (file.exists()) {
                    filePaths.add(filePath);
                } else {
                    return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Файл не найден"));
                }
            }


            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(filePaths);
        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }



    @PutMapping("/changeDate")
    @Operation(
            summary = "Редактирование даты пропуска",
            description = "Доступно только для студента",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Даты пропуска успешно обновлены\"\n}"))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> changeDate (HttpServletRequest request, @RequestParam UUID skippingRequestId, @RequestParam LocalDate newStartDate, @RequestParam(required = false) LocalDate newEndDate){
        try{
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

            Optional<SkippingRequest> skippingRequest = skippingRequestRepository.findById(skippingRequestId);
            if (skippingRequest.isEmpty()){
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пропуск не найден"));
            }

            SkippingRequest newSkippingRequest = skippingRequest.get();
            if (!newSkippingRequest.getStudent().equals(user)){
                return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы не можете редактировать этот пропуск, так как не являетесь его владельцем"));
            }

            if (newSkippingRequest.getStatus() == SkippingRequestStatus.APPROVED || newSkippingRequest.getStatus() == SkippingRequestStatus.REJECTED){
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Нельзя изменить дату у одобренного или отклонённого пропуска"));
            }

            if (newEndDate != null && newStartDate.isAfter(newEndDate)){
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Дата начала не может быть позже даты окончания"));
            }

            newSkippingRequest.setStartDate(newStartDate);
            newSkippingRequest.setEndDate(newEndDate);
            skippingRequestRepository.save(newSkippingRequest);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "message", "Даты пропуска успешно обновлены",
                            "newStartDate", newStartDate,
                            "newEndDate", (newEndDate != null ? newEndDate.toString() : "Неограниченный")
                    ));

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }

}
