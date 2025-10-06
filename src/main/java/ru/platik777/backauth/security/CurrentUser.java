package ru.platik777.backauth.security;

import java.lang.annotation.*;

/**
 * Аннотация для автоматического извлечения userId из JWT токена
 *
 * Использование:
 * <pre>
 * {@code
 * @GetMapping("/api/v1/user")
 * public ResponseEntity<UserResponse> getUser(@CurrentUser Integer userId) {
 *     return ResponseEntity.ok(authService.getUser(userId));
 * }
 * }
 * </pre>
 *
 * Вместо:
 * <pre>
 * {@code
 * @GetMapping("/api/v1/user")
 * public ResponseEntity<UserResponse> getUser(@RequestHeader("userId") Integer userId) {
 *     // Небезопасно! userId можно подделать
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}