package com.example.ums.exception;

/** Becomes 409 Conflict: a duplicate username, e-mail or roll, or deleting the last admin. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
