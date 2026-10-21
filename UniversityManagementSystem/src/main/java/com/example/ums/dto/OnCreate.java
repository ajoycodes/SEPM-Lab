package com.example.ums.dto;

/**
 * Validation group for rules that apply only when creating (POST), such as "password is required".
 * On update (PUT) the password may be left out to keep the old one.
 */
public interface OnCreate {
}
