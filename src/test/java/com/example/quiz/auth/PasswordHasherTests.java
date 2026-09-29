package com.example.quiz.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTests {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesPasswordsAndVerifiesOnlyTheMatchingPassword() {
        String encoded = passwordHasher.hash("correct horse battery staple");

        assertNotEquals("correct horse battery staple", encoded);
        assertTrue(passwordHasher.matches("correct horse battery staple", encoded));
        assertFalse(passwordHasher.matches("incorrect password", encoded));
    }
}
