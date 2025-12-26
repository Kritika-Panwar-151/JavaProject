package com.example.attendance.service;

import com.example.attendance.dto.StudentAttendanceRequest;
import com.example.attendance.util.DistanceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", key);
        h.set("Authorization", "Bearer " + key);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public String markAttendance(StudentAttendanceRequest req) {
        try {
            String q = url + "/rest/v1/attendance_sessions?class_code=eq." + req.getClassCode();

            List<Map<String, Object>> sessions = rt.exchange(
                    q, HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    List.class
            ).getBody();

            if (sessions == null || sessions.isEmpty())
                return "INVALID_CODE";

            Map<String, Object> session = sessions.get(0);

            if (!"ACTIVE".equals(session.get("status")))
                return "SESSION_NOT_ACTIVE";

            if (session.get("latitude") == null || session.get("longitude") == null)
                return "SESSION_LOCATION_NOT_SET";

            double tLat = ((Number) session.get("latitude")).doubleValue();
            double tLon = ((Number) session.get("longitude")).doubleValue();

            if (DistanceUtil.distanceMeters(
                    tLat, tLon,
                    req.getLatitude(), req.getLongitude()) > 50)
                return "OUT_OF_RANGE";

            Map<String, Object> record = new HashMap<>();
            record.put("session_id", session.get("id"));
            record.put("usn", req.getUsn());
            record.put("device_id", req.getDeviceId());
            record.put("latitude", req.getLatitude());
            record.put("longitude", req.getLongitude());

            rt.postForEntity(
                    url + "/rest/v1/attendance_records",
                    new HttpEntity<>(record, headers()),
                    String.class
            );

            return "SUCCESS";

        } catch (Exception e) {
            return "SERVER_ERROR";
        }
    }
}
