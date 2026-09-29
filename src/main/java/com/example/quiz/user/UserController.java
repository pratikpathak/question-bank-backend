package com.example.quiz.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // GET Endpoint to retrieve all users in JSON format
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
