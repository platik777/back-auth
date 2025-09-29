package ru.platik777.backauth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для отправки сообщения в поддержку
 * Соответствует SendMessageToSupport из Go
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageToSupportRequestDto {

    /**
     * Имя отправителя
     */
    @NotBlank(message = "Sender name cannot be empty")
    private String senderName;

    /**
     * Email отправителя
     */
    @NotBlank(message = "Sender email cannot be empty")
    @Email(message = "Invalid email format")
    private String senderEmail;

    /**
     * Текст сообщения
     */
    @NotBlank(message = "Message cannot be empty")
    private String message;

    /**
     * Тема сообщения (для какого продукта)
     */
    private String targetSubject;
}