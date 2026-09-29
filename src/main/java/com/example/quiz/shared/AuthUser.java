package com.example.quiz.shared;

import java.io.Serializable;

public record AuthUser(long id, String email, String role) implements Serializable {
}
