package com.example.quiz.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER") // Maps exactly to your USER table
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @Column(name = "crated_at") // Matches your exact column spelling
    private LocalDateTime cratedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCratedAt() { return cratedAt; }
    public void setCratedAt(LocalDateTime cratedAt) { this.cratedAt = cratedAt; }
}
