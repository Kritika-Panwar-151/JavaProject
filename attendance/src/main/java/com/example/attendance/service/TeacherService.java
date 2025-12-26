package com.example.attendance.service;

import com.example.attendance.dto.GenerateSessionRequest;
import com.example.attendance.dto.TeacherLoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TeacherService {

    private final RestTemplate rt;

    // 🔥 THIS CONSTRUCTOR IS MANDATORY
    public TeacherService() {
        this.rt = new RestTemplate(
                new HttpComponentsClientHttpRequestFactory()
        );
    }

    @Value("${SUPABASE_URL}")
    private String url;

    @Value("${SUPABASE_KEY}")
    private String key;

    /* ================= COMMON HEADERS ================= */
    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", key);
        h.set("Authorization", "Bearer " + key);
        h.setContentType(MediaType.APPLICATION_JSON);

        // REQUIRED for Supabase PATCH
        h.set("Prefer", "return=representation");

        return h;
    }

    /* ================= LOGIN ================= */
    public String login(TeacherLoginRequest req) {

        String q = url + "/rest/v1/teachers?teacher_id=eq." + req.getTeacherId();

        List<Map<String, Object>> res = rt.exchange(
                q,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class
        ).getBody();

        if (res == null || res.isEmpty())
            return "TEACHER_NOT_FOUND";

        Map<String, Object> teacher = res.get(0);

        return req.getPassword().equals(teacher.get("password_hash"))
                ? "SUCCESS"
                : "INVALID_PASSWORD";
    }

    /* ================= GENERATE SESSION ================= */
    public String generateSession(GenerateSessionRequest req) {

        String code = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        Map<String, Object> body = new HashMap<>();
        body.put("class_code", code);
        body.put("section", req.getSection());
        body.put("subject_code", req.getSubjectCode());
        body.put("status", "CREATED");

        rt.postForEntity(
                url + "/rest/v1/attendance_sessions",
                new HttpEntity<>(body, headers()),
                String.class
        );

        return code;
    }

    /* ================= UPDATE STATUS ================= */
    public void updateStatus(String code, String status) {

        String fetchUrl = url + "/rest/v1/attendance_sessions?class_code=eq." + code;

        List<Map<String, Object>> sessions = rt.exchange(
                fetchUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class
        ).getBody();

        if (sessions == null || sessions.isEmpty())
            return;

        Object sessionId = sessions.get(0).get("id");

        String patchUrl = url + "/rest/v1/attendance_sessions?id=eq." + sessionId;

        Map<String, Object> body = new HashMap<>();
        body.put("status", status);

        rt.exchange(
                patchUrl,
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers()),
                String.class
        );
    }

    /* ================= START SESSION ================= */
    public void startSessionWithLocation(
            String code, Double latitude, Double longitude) {

        String fetchUrl = url + "/rest/v1/attendance_sessions?class_code=eq." + code;

        List<Map<String, Object>> sessions = rt.exchange(
                fetchUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class
        ).getBody();

        if (sessions == null || sessions.isEmpty())
            return;

        Object sessionId = sessions.get(0).get("id");

        String patchUrl = url + "/rest/v1/attendance_sessions?id=eq." + sessionId;

        Map<String, Object> body = new HashMap<>();
        body.put("status", "ACTIVE");
        body.put("latitude", latitude);
        body.put("longitude", longitude);

        rt.exchange(
                patchUrl,
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers()),
                String.class
        );
    }

    /* ================= GET ATTENDANCE ================= */
    public List<Map<String, Object>> getAttendanceForClass(String classCode) {

        String sUrl = url + "/rest/v1/attendance_sessions?class_code=eq." + classCode;

        List<Map<String, Object>> sessions = rt.exchange(
                sUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class
        ).getBody();

        if (sessions == null || sessions.isEmpty())
            return new ArrayList<>();

        Map<String, Object> session = sessions.get(0);

        String aUrl = url + "/rest/v1/attendance_records"
                + "?session_id=eq." + session.get("id");

        List<Map<String, Object>> records = rt.exchange(
                aUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class
        ).getBody();

        return records == null ? new ArrayList<>() : records;
    }

    public byte[] generateAttendanceCsv(String classCode) {

    // 1. Get attendance records (already exists)
    List<Map<String, Object>> records = getAttendanceForClass(classCode);

    // 2. Build CSV text
    StringBuilder sb = new StringBuilder();
    sb.append("USN\n");

    for (Map<String, Object> r : records) {
        sb.append(r.get("usn")).append("\n");
    }
    // 3. Convert to bytes
    return sb.toString().getBytes();
}

}
