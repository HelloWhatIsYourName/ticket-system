package com.example.aiticket.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {
    @Test
    void invalidLoginExceptionMapsToUnauthorizedFailureResponse() throws Exception {
        Method method = AuthController.class.getMethod("invalidLogin", InvalidLoginException.class);

        assertThat(method.getAnnotation(ExceptionHandler.class).value())
                .containsExactly(InvalidLoginException.class);
        assertThat(method.getAnnotation(ResponseStatus.class).value())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        AuthController controller = new AuthController(null);

        assertThat(controller.invalidLogin(new InvalidLoginException("用户名或密码错误")).success()).isFalse();
        assertThat(controller.invalidLogin(new InvalidLoginException("用户名或密码错误")).message())
                .isEqualTo("用户名或密码错误");
    }
}
