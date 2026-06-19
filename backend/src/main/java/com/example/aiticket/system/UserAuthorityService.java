package com.example.aiticket.system;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class UserAuthorityService {
    private final UserSecurityMapper mapper;

    public UserAuthorityService(UserSecurityMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<UserAuthoritySnapshot> loadByUsername(String username) {
        return mapper.findActiveUserByUsername(username).map(this::snapshot);
    }

    public Optional<UserAuthoritySnapshot> loadByUserId(Long userId) {
        return mapper.findActiveUserById(userId).map(this::snapshot);
    }

    private UserAuthoritySnapshot snapshot(UserAccount user) {
        List<String> roles = distinctSorted(mapper.findRoleCodesByUserId(user.id()));
        List<String> permissions = distinctSorted(mapper.findPermissionCodesByUserId(user.id()));
        List<MenuSummary> menus = List.copyOf(new LinkedHashSet<>(mapper.findVisibleMenusByUserId(user.id())));
        return new UserAuthoritySnapshot(user, roles, permissions, menus);
    }

    private List<String> distinctSorted(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
