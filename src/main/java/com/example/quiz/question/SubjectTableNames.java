package com.example.quiz.question;

import java.util.List;
import java.util.Locale;

public final class SubjectTableNames {

    private static final List<String> ALLOWED_SUBJECTS =
            List.of("KOTLIN", "JAVA", "AI", "REACT", "PYTHON", "SPRING_BOOT");

    private SubjectTableNames() {
    }

    public static String resolve(String subject) {
        if (subject == null) {
            return null;
        }
        String normalized = subject.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        return ALLOWED_SUBJECTS.contains(normalized) ? normalized + "_TABLE" : null;
    }
}
