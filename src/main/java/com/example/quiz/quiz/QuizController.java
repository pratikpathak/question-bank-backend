package com.example.quiz.quiz;

import com.example.quiz.question.SubjectTableNames;
import com.example.quiz.shared.SessionUsers;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/questions/quiz")
public class QuizController {

    private static final String QUIZ_SESSION_ATTRIBUTE = QuizController.class.getName() + ".quiz";
    private final JdbcTemplate jdbcTemplate;

    public QuizController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/start/{subject}")
    public ResponseEntity<Map<String, Object>> startQuiz(
            @PathVariable String subject,
            HttpSession session) {
        if (SessionUsers.currentUser(session) == null) {
            return error(HttpStatus.UNAUTHORIZED, "Please sign in to start a quiz.");
        }
        String tableName = SubjectTableNames.resolve(subject);
        if (tableName == null) {
            return error(HttpStatus.BAD_REQUEST, "Invalid subject selected.");
        }

        try {
            if (!tableExists(tableName)) {
                session.removeAttribute(QUIZ_SESSION_ATTRIBUTE);
                return error(HttpStatus.NOT_FOUND, "No data found for this subject.");
            }
            ensureCorrectOptionColumn(tableName);
            List<QuizQuestion> questions = jdbcTemplate.query(
                    "SELECT id, question, option_1, option_2, option_3, option_4, "
                            + "TRIM(correct_option) AS quiz_answer FROM " + tableName
                            + " WHERE UPPER(TRIM(correct_option)) IN ('A', 'B', 'C', 'D') ORDER BY id",
                    (rs, rowNum) -> new QuizQuestion(
                            rs.getLong("id"),
                            rs.getString("question"),
                            rs.getString("option_1"),
                            rs.getString("option_2"),
                            rs.getString("option_3"),
                            rs.getString("option_4"),
                            rs.getString("quiz_answer")
                    )
            );
            if (questions.isEmpty()) {
                session.removeAttribute(QUIZ_SESSION_ATTRIBUTE);
                return error(HttpStatus.NOT_FOUND, "No questions with answers were found for this subject.");
            }

            QuizState state = new QuizState(List.copyOf(questions), 0);
            session.setAttribute(QUIZ_SESSION_ATTRIBUTE, state);
            return ResponseEntity.ok(Map.of(
                    "status", "Success",
                    "message", "Quiz started.",
                    "question", publicQuestion(state.current()),
                    "totalQuestions", questions.size(),
                    "questionNumber", 1
            ));
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not start the quiz.");
        }
    }

    @PostMapping("/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @RequestBody Map<String, String> payload,
            HttpSession session) {
        if (SessionUsers.currentUser(session) == null) {
            return error(HttpStatus.UNAUTHORIZED, "Please sign in to continue the quiz.");
        }
        Object value = session.getAttribute(QUIZ_SESSION_ATTRIBUTE);
        if (!(value instanceof QuizState state)) {
            return error(HttpStatus.BAD_REQUEST, "Start a quiz before submitting an answer.");
        }

        QuizQuestion current = state.current();
        if (!isCorrectAnswer(current.correctOption(), payload.get("answer"))) {
            return ResponseEntity.ok(Map.of(
                    "status", "Success",
                    "correct", false,
                    "message", "That answer is not correct. Try again.",
                    "question", publicQuestion(current),
                    "questionNumber", state.index() + 1,
                    "totalQuestions", state.questions().size()
            ));
        }

        int nextIndex = state.index() + 1;
        if (nextIndex >= state.questions().size()) {
            session.removeAttribute(QUIZ_SESSION_ATTRIBUTE);
            return ResponseEntity.ok(Map.of(
                    "status", "Success",
                    "correct", true,
                    "complete", true,
                    "message", "Correct! You completed the quiz.",
                    "questionNumber", nextIndex,
                    "totalQuestions", state.questions().size()
            ));
        }

        QuizState nextState = new QuizState(state.questions(), nextIndex);
        session.setAttribute(QUIZ_SESSION_ATTRIBUTE, nextState);
        return ResponseEntity.ok(Map.of(
                "status", "Success",
                "correct", true,
                "complete", false,
                "message", "Correct! Here is the next question.",
                "question", publicQuestion(nextState.current()),
                "questionNumber", nextIndex + 1,
                "totalQuestions", state.questions().size()
        ));
    }

    public static boolean isCorrectAnswer(String correctOption, String selectedOption) {
        return isAnswerLetter(correctOption)
                && isAnswerLetter(selectedOption)
                && correctOption.trim().equalsIgnoreCase(selectedOption.trim());
    }

    private static boolean isAnswerLetter(String answer) {
        return answer != null && List.of("A", "B", "C", "D").contains(answer.trim().toUpperCase(Locale.ROOT));
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
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = ? AND column_name = 'correct_option'",
                Integer.class,
                tableName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN correct_option VARCHAR(255) NULL");
        }
    }

    private Map<String, Object> publicQuestion(QuizQuestion question) {
        return Map.of(
                "id", question.id(),
                "question", question.question(),
                "option_1", question.option1(),
                "option_2", question.option2(),
                "option_3", question.option3(),
                "option_4", question.option4()
        );
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", "Error",
                "message", message
        ));
    }

    private record QuizQuestion(
            long id,
            String question,
            String option1,
            String option2,
            String option3,
            String option4,
            String correctOption
    ) implements Serializable {
    }

    private record QuizState(List<QuizQuestion> questions, int index) implements Serializable {
        QuizQuestion current() {
            return questions.get(index);
        }
    }
}
