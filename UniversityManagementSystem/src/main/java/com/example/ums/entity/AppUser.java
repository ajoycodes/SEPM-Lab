package com.example.ums.entity;

import jakarta.persistence.*;

/**
 * Fields every person who can log in has. Admin, Teacher and Student extend this class.
 *
 * JOINED inheritance: this class is the "users" table (one row per person, used for login),
 * and each subclass has its own table holding only its extra columns, linked by the same id.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt hash. The plain password is never stored. */
    @Column(nullable = false, length = 72)
    private String password;

    /** Set by the subclass constructor, never taken from a request. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private Role role;

    protected AppUser() {
    }

    protected AppUser(Role role) {
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }
}
