package com.example.aiticket.system;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserSecurityMapperXmlTest {
    @Test
    void auditLogInsertDeclaresJdbcTypesForNullableColumns() throws Exception {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/UserSecurityMapper.xml"));

        assertThat(mapper).contains("#{actorUserId,jdbcType=NUMERIC}");
        assertThat(mapper).contains("#{actorUsername,jdbcType=VARCHAR}");
        assertThat(mapper).contains("#{targetId,jdbcType=VARCHAR}");
        assertThat(mapper).contains("#{message,jdbcType=VARCHAR}");
        assertThat(mapper).contains("#{ipAddress,jdbcType=VARCHAR}");
        assertThat(mapper).contains("#{userAgent,jdbcType=VARCHAR}");
    }
}
