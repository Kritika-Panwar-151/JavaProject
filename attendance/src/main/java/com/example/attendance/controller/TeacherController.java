package com.example.attendance.controller;

import com.example.attendance.entity.TeacherSession;
import com.example.attendance.repository.TeacherSessionRepository;
import com.example.attendance.util.CodeGenerator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin
public class TeacherController {

    private final TeacherSessionRepository repo;

    public TeacherController(TeacherSessionRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/generate")
    public TeacherSession generateSession(@RequestBody TeacherSession session) {

        String code = CodeGenerator.generateCode(6);
        session.setClassCode(code);
        session.setActive(true);

        return repo.save(session);
    }
}
