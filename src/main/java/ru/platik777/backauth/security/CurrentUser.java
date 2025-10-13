package ru.platik777.backauth.security;

import java.lang.annotation.*;

/**
 * Аннотация для автоматического извлечения userId из JWT токена
 * <p>
 * Использование:
 * <pre>
 * {@code
 * @GetMapping("/api/v1/user")
 * public ResponseEntity<UserResponse> getUser(@CurrentUser Integer userId) {
 *     return ResponseEntity.ok(authService.getUser(userId));
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}