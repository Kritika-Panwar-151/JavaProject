package com.example.attendance.service;

import com.example.attendance.dto.StudentAttendanceRequest;
import com.example.attendance.util.DistanceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentService {

    private final RestTemplate rt = new RestTemplate();

    @Value("${SUPABASE_URL}")
    private String url;

    @Value("${SUPABASE_KEY}")
    private String key;

    /* -------------------- COMMON HEADERS -------------------- */
    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", key);
        h.set("Authorization", "Bearer " + key);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /* -------------------- MARK ATTENDANCE -------------------- */
    public String markAttendance(StudentAttendanceRequest req) {
        try {
            // 1. Fetch attendance session using code
            String q = url + "/rest/v1/attendance_sessions?class_code=eq." + req.getClassCode();

            List<Map<String, Object>> sessions = rt.exchange(
                    q, HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    List.class
            ).getBody();

            if (sessions == null || sessions.isEmpty())
                return "INVALID_CODE";

            Map<String, Object> session = sessions.get(0);

            // 2. Session must be active
            if (!"ACTIVE".equals(session.get("status")))
                return "SESSION_NOT_ACTIVE";

            // 3. Teacher location must be stored
            if (session.get("latitude") == null || session.get("longitude") == null)
                return "SESSION_LOCATION_NOT_SET";

            double tLat = ((Number) session.get("latitude")).doubleValue();
            double tLon = ((Number) session.get("longitude")).doubleValue();

            // 4. Distance check - student should be within 50 meters
            if (DistanceUtil.distanceMeters(
                    tLat, tLon,
                    req.getLatitude(), req.getLongitude()
            ) > 50)
                return "OUT_OF_RANGE";

            // 5. Check if student already marked attendance
            String checkUrl = url + "/rest/v1/attendance_records"
                    + "?session_id=eq." + session.get("id")
                    + "&usn=eq." + req.getUsn();

            List<Map<String, Object>> existing = rt.exchange(
                    checkUrl, HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    List.class
            ).getBody();

            if (existing != null && !existing.isEmpty())
                return "ALREADY_MARKED";

            // 6. Save new attendance record
            Map<String, Object> record = new HashMap<>();
            record.put("session_id", session.get("id"));
            record.put("usn", req.getUsn());
            record.put("device_id", req.getDeviceId());
            record.put("latitude", req.getLatitude());
            record.put("longitude", req.getLongitude());
            record.put("status", "PRESENT");
            record.put("marked_at", new Date().toString());

            rt.postForEntity(
                    url + "/rest/v1/attendance_records",
                    new HttpEntity<>(record, headers()),
                    String.class
            );

            return "SUCCESS";

        } catch (Exception e) {
            e.printStackTrace(); // Keep this for debugging
            return "SERVER_ERROR: " + e.getMessage();
        }
    }
}
