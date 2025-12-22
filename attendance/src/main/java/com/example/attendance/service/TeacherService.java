package com.example.attendance.service;

import com.example.attendance.dto.GenerateSessionRequest;
import com.example.attendance.dto.TeacherLoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeacherService {

    @Value("${SUPABASE_URL}")
    private String url;

    @Value("${SUPABASE_KEY}")
    private String key;

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", key);
        h.set("Authorization", "Bearer " + key);
        h.set("Prefer", "return=minimal");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public String login(TeacherLoginRequest req) {

        String q = url + "/rest/v1/teachers?teacher_id=eq." + req.getTeacherId();

        List res = new RestTemplate().exchange(
                q, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class).getBody();

        if (res == null || res.isEmpty())
            return "TEACHER_NOT_FOUND";

        Map teacher = (Map) res.get(0);

        return req.getPassword().equals(teacher.get("password_hash"))
                ? "SUCCESS"
                : "INVALID_PASSWORD";
    }

    public String generateSession(GenerateSessionRequest req) {

        String code = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        Map<String, Object> body = Map.of(
                "class_code", code,
                "section", req.getSection(),
                "subject_code", req.getSubjectCode(),
                "status", "CREATED"
        );

        new RestTemplate().postForEntity(
                url + "/rest/v1/attendance_sessions",
                new HttpEntity<>(body, headers()),
                String.class
        );

        return code;
    }

    public void updateStatus(String code, String status) {

        Map<String, Object> body = Map.of("status", status);

        new RestTemplate().exchange(
                url + "/rest/v1/attendance_sessions?class_code=eq." + code,
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers()),
                String.class
        );
    }
    public List<Map<String, Object>> getAttendanceForClass(String classCode) {

        RestTemplate rt = new RestTemplate();

        // 1. Get session by class code
        String sUrl = url + "/rest/v1/attendance_sessions?class_code=eq." + classCode;
        List sessions = rt.exchange(
                sUrl, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class
        ).getBody();

        if (sessions == null || sessions.isEmpty())
            return List.of();

        Map session = (Map) sessions.get(0);

        // 2. Get attendance records for that session
        String aUrl = url + "/rest/v1/attendance_records"
                + "?session_id=eq." + session.get("id");

        return rt.exchange(
                aUrl, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class
        ).getBody();
}

}
