package com.example.attendance.controller;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;

import com.example.attendance.dto.*;
import com.example.attendance.service.TeacherService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @RequestBody TeacherLoginRequest request) {

        return ResponseEntity.ok(
                new ApiResponse(teacherService.login(request))
        );
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(
            @RequestBody GenerateSessionRequest request) {

        Map<String, String> response = new HashMap<>();
        response.put("classCode", teacherService.generateSession(request));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start/{code}")
    public ResponseEntity<ApiResponse> startSession(
            @PathVariable String code,
            @RequestBody Map<String, Double> body) {

        Double latitude = body.get("latitude");
        Double longitude = body.get("longitude");

        if (latitude == null || longitude == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("LAT_LONG_REQUIRED"));
        }

        teacherService.startSessionWithLocation(code, latitude, longitude);
        return ResponseEntity.ok(new ApiResponse("SESSION_STARTED"));
    }

    @PostMapping("/pause/{code}")
    public ResponseEntity<ApiResponse> pause(@PathVariable String code) {
        teacherService.updateStatus(code, "PAUSED");
        return ResponseEntity.ok(new ApiResponse("SESSION_PAUSED"));
    }

    @PostMapping("/resume/{code}")
    public ResponseEntity<ApiResponse> resume(@PathVariable String code) {
        teacherService.updateStatus(code, "ACTIVE");
        return ResponseEntity.ok(new ApiResponse("SESSION_RESUMED"));
    }

    @PostMapping("/end/{code}")
    public ResponseEntity<ApiResponse> end(@PathVariable String code) {
        teacherService.updateStatus(code, "ENDED");
        return ResponseEntity.ok(new ApiResponse("SESSION_ENDED"));
    }

    @GetMapping("/attendance/{classCode}")
    public ResponseEntity<?> getAttendance(@PathVariable String classCode) {
        return ResponseEntity.ok(
                teacherService.getAttendanceForClass(classCode)
        );
    }

   @GetMapping("/attendance/download/{classCode}")
public ResponseEntity<byte[]> downloadAttendance(
        @PathVariable String classCode) {

    byte[] csv = teacherService.generateAttendanceCsv(classCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType("text/csv; charset=UTF-8")
    );
    headers.set(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"attendance_" + classCode + ".csv\""
    );

    return new ResponseEntity<>(csv, headers, HttpStatus.OK);
}


}
