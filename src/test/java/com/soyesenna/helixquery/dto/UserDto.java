package com.soyesenna.helixquery.dto;

/**
 * DTO for User projection tests.
 */
public class UserDto {

    private final String name;
    private final String email;

    public UserDto(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "UserDto{name='" + name + "', email='" + email + "'}";
    }
}
