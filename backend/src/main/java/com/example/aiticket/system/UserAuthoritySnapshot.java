package com.example.aiticket.system;

import java.util.List;

public record UserAuthoritySnapshot(
        UserAccount user,
        List<String> roles,
        List<String> permissions,
        List<MenuSummary> menus
) {
}
