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
        h.set("Prefer", "return=minimal");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public String markAttendance(StudentAttendanceRequest req) {

        RestTemplate rt = new RestTemplate();

        // 1. Fetch session
        String q = url + "/rest/v1/attendance_sessions?class_code=eq." + req.getClassCode();
        List sessions = rt.exchange(q, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class).getBody();

        if (sessions == null || sessions.isEmpty())
            return "INVALID_CODE";

        Map session = (Map) sessions.get(0);

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

        // 2. Check duplicate USN
        String usnCheck = url + "/rest/v1/attendance_records"
                + "?session_id=eq." + session.get("id")
                + "&usn=eq." + req.getUsn();

        List usn = rt.exchange(usnCheck, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class).getBody();

        if (usn != null && !usn.isEmpty())
            return "ALREADY_MARKED";

        // 3. Check device
        String devCheck = url + "/rest/v1/attendance_records"
                + "?session_id=eq." + session.get("id")
                + "&device_id=eq." + req.getDeviceId();

        List dev = rt.exchange(devCheck, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class).getBody();

        if (dev != null && !dev.isEmpty())
            return "DEVICE_MISMATCH";

        // 4. Insert attendance
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
