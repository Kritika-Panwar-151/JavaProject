package com.example.attendance.service;

import com.example.attendance.dto.StudentAttendanceRequest;
import com.example.attendance.util.DistanceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

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
            // 1. Fetch Session
            String q = url + "/rest/v1/attendance_sessions?class_code=eq." + req.getClassCode();
            List<Map<String, Object>> s = rt.exchange(q, HttpMethod.GET, new HttpEntity<>(headers()), List.class).getBody();

            if (s == null || s.isEmpty()) return "INVALID_CODE";
            Map<String, Object> session = s.get(0);

            if (!"ACTIVE".equals(session.get("status"))) return "SESSION_NOT_ACTIVE";
            if (session.get("latitude") == null) return "SESSION_LOCATION_NOT_SET";

            // 2. Distance Check - 50m Limit
            double tLat = ((Number) session.get("latitude")).doubleValue();
            double tLon = ((Number) session.get("longitude")).doubleValue();
            double distance = DistanceUtil.distanceMeters(tLat, tLon, req.getLatitude(), req.getLongitude());

            System.out.println("DEBUG: USN " + req.getUsn() + " is " + String.format("%.2f", distance) + "m away.");

            if (distance > 50) return "OUT_OF_RANGE";

            // 3. Device Binding & Proxy Prevention
            // Check A: Does this USN belong to another device?
            String usnUrl = url + "/rest/v1/student_devices?usn=eq." + req.getUsn();
            List<Map<String, Object>> usnMap = rt.exchange(usnUrl, HttpMethod.GET, new HttpEntity<>(headers()), List.class).getBody();

            if (usnMap != null && !usnMap.isEmpty()) {
                if (!usnMap.get(0).get("device_id").equals(req.getDeviceId())) {
                    return "DEVICE_NOT_MATCHED";
                }
            } else {
                // Check B: Does this Device belong to another USN?
                String devUrl = url + "/rest/v1/student_devices?device_id=eq." + req.getDeviceId();
                List<Map<String, Object>> devMap = rt.exchange(devUrl, HttpMethod.GET, new HttpEntity<>(headers()), List.class).getBody();

                if (devMap != null && !devMap.isEmpty()) {
                    return "DEVICE_ALREADY_REGISTERED";
                }

                // New Registration (Supabase requires List for POST)
                List<Map<String, Object>> insert = new ArrayList<>();
                Map<String, Object> data = new HashMap<>();
                data.put("usn", req.getUsn());
                data.put("device_id", req.getDeviceId());
                insert.add(data);
                rt.postForEntity(url + "/rest/v1/student_devices", new HttpEntity<>(insert, headers()), String.class);
            }

            // 4. Duplicate Check
            String dupUrl = url + "/rest/v1/attendance_records?session_id=eq." + session.get("id") + "&usn=eq." + req.getUsn();
            List<Map<String, Object>> exist = rt.exchange(dupUrl, HttpMethod.GET, new HttpEntity<>(headers()), List.class).getBody();
            if (exist != null && !exist.isEmpty()) return "ALREADY_MARKED";

            // 5. Save Record
            Map<String, Object> record = new HashMap<>();
            record.put("session_id", session.get("id"));
            record.put("usn", req.getUsn());
            record.put("device_id", req.getDeviceId());
            record.put("latitude", req.getLatitude());
            record.put("longitude", req.getLongitude());
            record.put("status", "PRESENT");

            rt.postForEntity(url + "/rest/v1/attendance_records", new HttpEntity<>(record, headers()), String.class);
            return "SUCCESS";

        } catch (Exception e) {
            e.printStackTrace();
            return "SERVER_ERROR";
        }
    }
}