package com.example.quiz.question;

import com.example.quiz.quiz.QuizController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionAnswerTests {

    @Test
    void comparesTheSelectedOptionLetterToTheStoredCorrectOption() {
        assertTrue(QuizController.isCorrectAnswer("A", "A"));
        assertTrue(QuizController.isCorrectAnswer("b", "B"));
        assertFalse(QuizController.isCorrectAnswer("A", "C"));
        assertFalse(QuizController.isCorrectAnswer("Answer A", "A"));
        assertFalse(QuizController.isCorrectAnswer("A", "Option A text"));
        assertFalse(QuizController.isCorrectAnswer("E", "E"));
        assertFalse(QuizController.isCorrectAnswer(null, "A"));
    }

    @Test
    void acceptsOnlyValidCorrectOptionLettersForNewQuestions() {
        assertTrue(QuestionController.isAnswerLetter("A"));
        assertTrue(QuestionController.isAnswerLetter("d"));
        assertFalse(QuestionController.isAnswerLetter("Option A"));
        assertFalse(QuestionController.isAnswerLetter("E"));
    }
}
