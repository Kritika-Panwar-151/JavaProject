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
            // 1. fetch session
            String q = url + "/rest/v1/attendance_sessions?class_code=eq." + req.getClassCode();
            List<Map<String, Object>> s = rt.exchange(q, HttpMethod.GET,
                    new HttpEntity<>(headers()), List.class).getBody();

            if (s == null || s.isEmpty()) return "INVALID_CODE";

            Map<String, Object> session = s.get(0);

            if (!"ACTIVE".equals(session.get("status"))) return "SESSION_NOT_ACTIVE";

            double tLat = ((Number) session.get("latitude")).doubleValue();
            double tLon = ((Number) session.get("longitude")).doubleValue();

            // 2. distance check 50 meters
            double distance = DistanceUtil.distanceMeters(tLat, tLon,
                    req.getLatitude(), req.getLongitude());

            System.out.println("\n===== LOCATION DEBUG =====");
            System.out.println("Teacher : " + tLat + "," + tLon);
            System.out.println("Student : " + req.getLatitude() + "," + req.getLongitude());
            System.out.println("Distance: " + distance + "m");
            System.out.println("==========================\n");

            if (distance > 50) return "OUT_OF_RANGE";

           // 3. DEVICE → USN mapping
            String mapUrl = url + "/rest/v1/student_devices?usn=eq." + req.getUsn();

            List<Map<String,Object>> mapData = rt.exchange(
                    mapUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    List.class
            ).getBody();

            System.out.println("📌 Checking device map for USN = " + req.getUsn());

            if(mapData == null || mapData.isEmpty()){
                System.out.println("⚠ No mapping exists — registering this device...");

                HttpHeaders hh = new HttpHeaders();
                hh.set("apikey", key);
                hh.set("Authorization", "Bearer " + key);
                hh.set("Prefer", "return=representation");      // 🔥 MUST HAVE
                hh.setContentType(MediaType.APPLICATION_JSON);

                Map<String,Object> insert = new HashMap<>();
                insert.put("usn", req.getUsn());
                insert.put("device_id", req.getDeviceId());

                try {
                    ResponseEntity<String> res = rt.postForEntity(
                            url + "/rest/v1/student_devices",
                            new HttpEntity<>(insert, hh),
                            String.class
                    );
                    System.out.println("✅ Device registered: " + res.getBody());

                } catch (Exception e){
                    System.out.println("❌ Error inserting device mapping:");
                    e.printStackTrace();                         // 🔥 show errors instead of hiding
                    return "DEVICE_SAVE_FAILED";
                }

            } else {
                String saved = (String) mapData.get(0).get("device_id");
                System.out.println("🔍 Saved Device = " + saved);
                System.out.println("🔍 Current Device = " + req.getDeviceId());

                if(!saved.equals(req.getDeviceId())){
                    System.out.println("❌ Device mismatch — blocking");
                    return "DEVICE_NOT_MATCHED";
                }
            }


            // 4. Prevent duplicate
            String check = url + "/rest/v1/attendance_records?session_id=eq."
                    + session.get("id") + "&usn=eq." + req.getUsn();

            List<Map<String,Object>> exist = rt.exchange(check, HttpMethod.GET,
                    new HttpEntity<>(headers()), List.class).getBody();

            if (exist != null && !exist.isEmpty()) return "ALREADY_MARKED";

            // 5. store attendance
            Map<String,Object> r = new HashMap<>();
            r.put("session_id", session.get("id"));
            r.put("usn", req.getUsn());
            r.put("device_id", req.getDeviceId());
            r.put("latitude", req.getLatitude());
            r.put("longitude", req.getLongitude());
            r.put("status","PRESENT");
            r.put("marked_at", new Date());

            rt.postForEntity(url + "/rest/v1/attendance_records",
                    new HttpEntity<>(r, headers()), String.class);

            return "SUCCESS";

        } catch (Exception e){
            e.printStackTrace();
            return "SERVER_ERROR";
        }
    }
}
