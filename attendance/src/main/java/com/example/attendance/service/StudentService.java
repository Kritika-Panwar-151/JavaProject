package com.example.attendance.service;

import com.example.attendance.dto.StudentAttendanceRequest;
import com.example.attendance.util.DistanceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class StudentService {

    @Value("${SUPABASE_URL}")
    private String url;

    @Value("${SUPABASE_KEY}")
    private String key;

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", key);
        h.set("Authorization", "Bearer " + key);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public String markAttendance(StudentAttendanceRequest req) {

        RestTemplate rt = new RestTemplate();

        // 1️⃣ Fetch session
        String q = url + "/rest/v1/attendance_sessions?class_code=eq."
                + req.getClassCode();

        List sessions = rt.exchange(
                q, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class
        ).getBody();

        if (sessions.isEmpty())
            return "INVALID_CODE";

        Map session = (Map) sessions.get(0);

        // 2️⃣ Check session status
        if (!"ACTIVE".equals(session.get("status")))
            return "SESSION_NOT_ACTIVE";

        // 3️⃣ Distance check
        double distance = DistanceUtil.distanceMeters(
                (double) session.get("latitude"),
                (double) session.get("longitude"),
                req.getLatitude(),
                req.getLongitude()
        );

        if (distance > 50)
            return "OUT_OF_RANGE";

        // 4️⃣ Prevent duplicate attendance
        String a = url + "/rest/v1/attendance_records"
                + "?session_id=eq." + session.get("id")
                + "&usn=eq." + req.getUsn();

        List existing = rt.exchange(
                a, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class
        ).getBody();

        if (!existing.isEmpty())
            return "ALREADY_MARKED";

        // 5️⃣ Save attendance
        Map<String, Object> record = Map.of(
                "session_id", session.get("id"),
                "usn", req.getUsn(),
                "device_id", req.getDeviceId(),
                "latitude", req.getLatitude(),
                "longitude", req.getLongitude()
        );

        rt.postForEntity(
                url + "/rest/v1/attendance_records",
                new HttpEntity<>(record, headers()),
                String.class
        );

        return "SUCCESS";
    }
}
