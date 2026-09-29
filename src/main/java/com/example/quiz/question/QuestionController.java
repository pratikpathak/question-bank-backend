package com.example.quiz.question;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.quiz.shared.AuthUser;
import com.example.quiz.shared.SessionUsers;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final JdbcTemplate jdbcTemplate;
    public QuestionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{subject}")
    public ResponseEntity<Map<String, Object>> getQuestions(
            @PathVariable String subject,
            HttpSession session) {
        if (!isStaff(SessionUsers.currentUser(session))) {
            return error(HttpStatus.FORBIDDEN, "Only administrators and data-entry operators can view answer keys.");
        }
        String tableName = SubjectTableNames.resolve(subject);
        if (tableName == null) {
            return error(HttpStatus.BAD_REQUEST, "Invalid subject selected.");
        }

        try {
            if (!tableExists(tableName)) {
                return error(HttpStatus.NOT_FOUND, "No data found: table '" + tableName + "' does not exist.");
            }
            ensureCorrectOptionColumn(tableName);
            List<Map<String, Object>> questions = jdbcTemplate.queryForList(
                    "SELECT id, question, option_1, option_2, option_3, option_4, correct_option, solution FROM " + tableName
            );
            return ResponseEntity.ok(Map.of(
                    "status", "Success",
                    "message", questions.isEmpty() ? "No questions found in table '" + tableName + "'." : "Questions retrieved successfully.",
                    "data", questions
            ));
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve questions.");
        }
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addQuestion(
            @RequestBody Map<String, String> payload,
            HttpSession session) {
        if (!isStaff(SessionUsers.currentUser(session))) {
            return error(HttpStatus.FORBIDDEN, "Only administrators and data-entry operators can add questions.");
        }

        String subject = payload.get("subject");
        String tableName = SubjectTableNames.resolve(subject);
        String question = payload.get("question");
        String option1 = payload.get("option1");
        String option2 = payload.get("option2");
        String option3 = payload.get("option3");
        String option4 = payload.get("option4");
        String correctOption = payload.get("correctOption");
        String solution = payload.get("solution");
        if (tableName == null) {
            return error(HttpStatus.BAD_REQUEST, "Invalid subject selected.");
        }
        if (isBlank(question) || isBlank(option1) || isBlank(option2) || isBlank(option3)
                || isBlank(option4) || isBlank(solution) || !isAnswerLetter(correctOption)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Provide a question, four options, the correct option letter (A, B, C, or D), and an explanation.");
        }

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "question TEXT NOT NULL, "
                    + "option_1 VARCHAR(255) NOT NULL, "
                    + "option_2 VARCHAR(255) NOT NULL, "
                    + "option_3 VARCHAR(255) NOT NULL, "
                    + "option_4 VARCHAR(255) NOT NULL, "
                    + "correct_option VARCHAR(255) NULL, "
                    + "solution TEXT NOT NULL"
                    + ")");
            ensureCorrectOptionColumn(tableName);
            jdbcTemplate.update(
                    "INSERT INTO " + tableName + " (question, option_1, option_2, option_3, option_4, correct_option, solution) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    question.trim(), option1.trim(), option2.trim(), option3.trim(), option4.trim(),
                    correctOption.trim(), solution.trim()
            );
            return ResponseEntity.ok(Map.of(
                    "status", "Success",
                    "message", "Question added to '" + tableName + "'."
            ));
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the question.");
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private void ensureCorrectOptionColumn(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'correct_option'",
                Integer.class,
                tableName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN correct_option VARCHAR(255) NULL");
        }
    }

    private boolean isStaff(AuthUser user) {
        return user != null && ("ADMIN".equals(user.role())
                || "DATA_ENTRY".equals(user.role()) || "OPERATOR".equals(user.role()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static boolean isAnswerLetter(String answer) {
        return answer != null && List.of("A", "B", "C", "D").contains(answer.trim().toUpperCase(Locale.ROOT));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", "Error",
                "message", message
        ));
    }
}
