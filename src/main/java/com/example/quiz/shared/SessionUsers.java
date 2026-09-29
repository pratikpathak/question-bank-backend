package com.example.quiz.shared;

import jakarta.servlet.http.HttpSession;

public final class SessionUsers {

    public static final String USER_SESSION_ATTRIBUTE = SessionUsers.class.getName() + ".user";

    private SessionUsers() {
    }

    public static AuthUser currentUser(HttpSession session) {
        Object user = session.getAttribute(USER_SESSION_ATTRIBUTE);
        return user instanceof AuthUser authUser ? authUser : null;
    }
}
