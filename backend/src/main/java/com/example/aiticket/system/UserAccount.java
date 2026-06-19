package com.example.aiticket.system;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String status,
        int tokenVersion
) {
    public boolean active() {
        return "ACTIVE".equals(status);
    }
}
