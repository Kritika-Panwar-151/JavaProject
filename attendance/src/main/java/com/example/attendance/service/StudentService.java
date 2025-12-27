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
            // 1. Fetch the session based on class code
            String q = url + "/rest/v1/attendance_sessions?class_code=eq." + req.getClassCode();
            List<Map<String, Object>> s = rt.exchange(q, HttpMethod.GET,
                    new HttpEntity<>(headers()), List.class).getBody();

            if (s == null || s.isEmpty()) return "INVALID_CODE";

            Map<String, Object> session = s.get(0);

            if (!"ACTIVE".equals(session.get("status"))) return "SESSION_NOT_ACTIVE";

            // Ensure teacher has set a location
            if (session.get("latitude") == null || session.get("longitude") == null) {
                return "SESSION_LOCATION_NOT_SET";
            }

            double tLat = ((Number) session.get("latitude")).doubleValue();
            double tLon = ((Number) session.get("longitude")).doubleValue();

            // 2. Distance check - Fixed at 50 meters
            double distance = DistanceUtil.distanceMeters(tLat, tLon,
                    req.getLatitude(), req.getLongitude());

            System.out.println("\n===== ATTENDANCE LOG =====");
            System.out.println("Student USN : " + req.getUsn());
            System.out.println("Calculated Distance: " + distance + "m");

            if (distance > 50) {
                System.out.println("Result: OUT_OF_RANGE");
                return "OUT_OF_RANGE";
            }

            // 3. Device Binding Logic (The student_devices table)
            String mapUrl = url + "/rest/v1/student_devices?usn=eq." + req.getUsn();

            List<Map<String, Object>> mapData = rt.exchange(
                    mapUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    List.class
            ).getBody();

            if (mapData == null || mapData.isEmpty()) {
                // FIRST TIME: Register this device to this USN
                System.out.println("Registering new device link...");

                HttpHeaders hh = headers();
                hh.set("Prefer", "return=representation");

                // Supabase POST requires a List (Array)
                List<Map<String, Object>> insertList = new ArrayList<>();
                Map<String, Object> insertMap = new HashMap<>();
                insertMap.put("usn", req.getUsn());
                insertMap.put("device_id", req.getDeviceId());
                insertList.add(insertMap);

                try {
                    rt.postForEntity(
                            url + "/rest/v1/student_devices",
                            new HttpEntity<>(insertList, hh),
                            String.class
                    );
                } catch (Exception e) {
                    System.err.println("Database Error: " + e.getMessage());
                    return "DEVICE_SAVE_FAILED";
                }
            } else {
                // NOT FIRST TIME: Verify the device ID matches the registered one
                String savedDeviceId = (String) mapData.get(0).get("device_id");
                if (!savedDeviceId.equals(req.getDeviceId())) {
                    System.out.println("Result: DEVICE_MISMATCH");
                    return "DEVICE_NOT_MATCHED";
                }
            }

            // 4. Prevent duplicate attendance for same USN in same Session
            String checkUrl = url + "/rest/v1/attendance_records?session_id=eq."
                    + session.get("id") + "&usn=eq." + req.getUsn();

            List<Map<String, Object>> exist = rt.exchange(checkUrl, HttpMethod.GET,
                    new HttpEntity<>(headers()), List.class).getBody();

            if (exist != null && !exist.isEmpty()) return "ALREADY_MARKED";

            // 5. Save the attendance record
            Map<String, Object> record = new HashMap<>();
            record.put("session_id", session.get("id"));
            record.put("usn", req.getUsn());
            record.put("device_id", req.getDeviceId());
            record.put("latitude", req.getLatitude());
            record.put("longitude", req.getLongitude());
            record.put("status", "PRESENT");
            // Supabase handles the marked_at default value (now())

            rt.postForEntity(url + "/rest/v1/attendance_records",
                    new HttpEntity<>(record, headers()), String.class);

            System.out.println("Result: SUCCESS");
            System.out.println("==========================\n");

            return "SUCCESS";

        } catch (Exception e) {
            e.printStackTrace();
            return "SERVER_ERROR";
        }
    }
}