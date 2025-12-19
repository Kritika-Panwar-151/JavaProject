package com.example.attendance.util;

import java.util.Random;

public class CodeGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String generate(int len) {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<len;i++)
            sb.append(CHARS.charAt(r.nextInt(CHARS.length())));
        return sb.toString();
    }
}
