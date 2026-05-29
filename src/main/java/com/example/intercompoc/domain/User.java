package com.example.intercompoc.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    private String role; // "CUSTOMER" or "AGENT"
}
