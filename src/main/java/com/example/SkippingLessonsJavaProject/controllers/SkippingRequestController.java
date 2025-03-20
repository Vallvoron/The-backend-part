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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @GetMapping("/skippingRequest")
    @Operation(
            summary = "Информация о пропуске",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SkippingRequest.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> skippingRequest (HttpServletRequest request, @RequestParam UUID skippingRequestId){
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
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пропуск не найден"));
            }

            Optional<SkippingRequest> skippingRequestOptional = skippingRequestRepository.findById(skippingRequestId);
            if (skippingRequestOptional.isEmpty()) {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Пользователь не наден"));
            }
            SkippingRequest skippingRequest= skippingRequestOptional.get();
            return  ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(skippingRequest);
        }catch (Exception error) {
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
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) SortSettings sortSetting,
            @RequestParam(required = false) Integer lessonNumber,
            @RequestParam(required = false, defaultValue = "false") Boolean isAppruved,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size){
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

            if (studentName != null && !studentName.isBlank()){
                finalList = finalList.stream()
                        .filter(stud -> stud.getStudent().getName() != null &&
                                stud.getStudent().getName().toLowerCase().contains(studentName.toLowerCase()))
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

            if (isAppruved) {
                finalList = finalList.stream()
                        .filter(t -> t.getStatus() == SkippingRequestStatus.APPROVED)
                        .collect(Collectors.toList());
            }

            int totalCount = finalList.size();
            int totalPages = (int) Math.ceil((double) totalCount / size);
            int fromIndex = (page - 1)*size;
            int toIndex = Math.min(fromIndex + size, finalList.size());

            if (fromIndex >= finalList.size()){
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Вы вышли за пределы списка"));
            }

            List<SkippingRequest> paginatedList = finalList.subList(fromIndex, toIndex);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of(
                    "totalCount", totalCount,
                    "totalPagesCount", totalPages,
                    "currentPage", page,
                    "pageSize", size,
                    "list", paginatedList));

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





    @PostMapping(value = "/addDocument", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Добавление документов к пропуску (создается новый документ)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Документ(-ы) успешно прикреплен(-ы)\"\n}"))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> createConfirmation(HttpServletRequest authRequest, @RequestParam("request") UUID request, @RequestParam("files") List<MultipartFile> files){


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
            Optional<SkippingRequest> skippingRequestOpt = skippingRequestRepository.findById(request);
            if (skippingRequestOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Пропуск не найден"));
            }
            SkippingRequest skippingRequest = skippingRequestOpt.get();

            if (user != skippingRequest.getStudent()) {
                return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав, вы не являетесь создателем пропуска"));
            }

            for(MultipartFile file : files) {
                String filename = file.getOriginalFilename();

                if (confirmationRepository.existsByFilenameAndSkippingRequest(filename, skippingRequest)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Файл с таким именем уже существует для этого пропуска"));
                }

                String filePath = uploadDirectory + "/" + UUID.randomUUID() + "_" + filename;
                System.out.println("Сохраняем файл по пути: " + filePath);

                Path path = Paths.get(filePath);
                Files.write(path, file.getBytes());

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

    @PostMapping("/attachDocumentToRequest")
    @Operation(
            summary = "Добавление документов к пропуску (существующие в бд документы добавляются к еще одному пропуску)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Документ(-ы) успешно прикреплен(-ы)\"\n}"))),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> attachDocumentToRequest(
            HttpServletRequest authRequest,
            @RequestParam List<UUID> confirmationsId,
            @RequestParam UUID skippingRequestId) {

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
            Optional<SkippingRequest> skippingRequestOpt = skippingRequestRepository.findById(skippingRequestId);
            if (skippingRequestOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Пропуск не найден"));
            }

            SkippingRequest skippingRequest = skippingRequestOpt.get();

            if (user != skippingRequest.getStudent()) {
                return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Запрос отклонен, у вас недостаточно прав, вы не являетесь создателем пропуска"));
            }

            for ( UUID confirmationId : confirmationsId)
            {
                Optional<Confirmation> confirmationOptional = confirmationRepository.findById(confirmationId);

                if (confirmationOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Документ не найден"));
                }
                Confirmation confirmation = confirmationOptional.get();

                confirmation.setSkippingRequest(skippingRequest);
                confirmationRepository.save(confirmation);
            }

            return ResponseEntity.ok(Map.of("message", "Документ(-ы) успешно прикреплен(-ы) к пропуску"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Ошибка: " + e.getMessage()));
        }
    }

    @GetMapping(value = "/getDocument", produces =  {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Получение листа документов",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",content = @Content(
                            mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary"),
                            examples = {
                                    @ExampleObject(
                                            name = "ZIP file",
                                            value = "Binary data of the ZIP file (cannot be displayed directly)"
                                    )
                            }
                    )),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> getConfirmationList(HttpServletRequest request,  @RequestParam UUID skippingRequestId) {

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

            Optional<SkippingRequest> skippingRequestOpt = skippingRequestRepository.findById(skippingRequestId);
            if (skippingRequestOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Пропуск не найден"));
            }
            SkippingRequest pass = skippingRequestOpt.get();
            List<Confirmation> confirmations = confirmationRepository.findBySkippingRequest(pass);


            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

            for (Confirmation confirmation : confirmations) {
                String filePath = confirmation.getFilePath();
                java.io.File file = new java.io.File(filePath);

                if (file.exists()) {

                    ZipEntry zipEntry = new ZipEntry(confirmation.getFilename());
                    zipOutputStream.putNextEntry(zipEntry);


                    Files.copy(file.toPath(), zipOutputStream);

                    zipOutputStream.closeEntry();
                } else {
                    return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Файл не найден"));
                }
            }


            zipOutputStream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "documents.zip"); // Set filename for the ZIP
            headers.setContentLength(byteArrayOutputStream.size());

            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
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

            if (newSkippingRequest.getStatus() == SkippingRequestStatus.REJECTED){
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Нельзя изменить дату у отклонённого пропуска"));
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


    @GetMapping("/export")
    @Operation(
            summary = "Экспорт пропусков",
            description = "Доступно только для админа и деканата",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content()),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content()),
                    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\n  \"message\": \"Ошибка: Описание ошибки\"\n}")))
            }
    )
    public ResponseEntity<?> export (HttpServletRequest request1){
        try(Workbook workbook = new XSSFWorkbook()){
            String authHeader = request1.getHeader("Authorization");

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

            Sheet sheet = workbook.createSheet("skippingRequest");

            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "ID Студента", "Дата начала", "Дата конца", "Причина", "Статус", "Порядковый номер урока", "ID Подтверждающего"};

            for (int i = 0; i < columns.length; i++){
                headerRow.createCell(i).setCellValue(columns[i]);
            }

            List<SkippingRequest> skippingRequests = skippingRequestRepository.findAll();

            int index = 1;
            for (SkippingRequest request : skippingRequests){
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(request.getId().toString());
                row.createCell(1).setCellValue(request.getStudent().getId().toString());
                row.createCell(2).setCellValue(request.getStartDate().toString());
                row.createCell(3).setCellValue(request.getEndDate().toString());
                row.createCell(4).setCellValue(request.getReason() != null ? request.getReason() : "");
                row.createCell(5).setCellValue(request.getStatus().name());
                row.createCell(6).setCellValue(request.getLessons() != null ? request.getLessons().toString() : "");
                row.createCell(7).setCellValue(request.getApprover() != null ? request.getApprover().getId().toString() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=skipping_requests.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception error) {
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "Ошибка: " + error.getMessage()));
        }
    }

}
