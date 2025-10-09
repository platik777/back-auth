package ru.platik777.backauth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Сервис отправки email
 * Соответствует функциям из smtp пакета Go
 *
 * Go аналоги:
 * - smtp.SendingMessage
 * - smtp.GetBodyMessageForResetPassword
 * - smtp.GetBodyMessageForSendMessageToSupport
 */
@Slf4j
// @Service
//@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;

    // Sender email берется из spring.mail.username
    @Value("${spring.mail.username}")
    private String senderEmail;

    // Support и product emails из app.smtp
    @Value("${app.smtp.support-email}")
    private String supportEmail;

    @Value("${app.smtp.product-email}")
    private String productEmail;

    // Флаг для отправки в логи (для dev/test окружений)
    @Value("${app.smtp.send-to-logs:false}")
    private boolean sendToLogs;

    public EmailService(JavaMailSender mailSender, ResourceLoader resourceLoader) {
        this.mailSender = mailSender;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Отправка письма для восстановления пароля
     * Go: func (a *AuthService) ResetPasswordForgot(...)
     *
     * @param targetEmail Email получателя
     * @param subject Тема письма
     * @param resetPasswordUrl URL для сброса пароля
     * @param login Логин пользователя
     * @param locale Язык письма (ru/en)
     */
    public void sendResetPasswordEmail(String targetEmail, String subject,
                                       String resetPasswordUrl,
                                       String login, String locale) {
        log.debug("Preparing reset password email for: {}", targetEmail);

        // Валидация входных данных
        validateEmail(targetEmail, "targetEmail");
        validateNotEmpty(resetPasswordUrl, "resetPasswordUrl");
        validateNotEmpty(subject, "subject");

        try {
            String htmlBody = getResetPasswordHtmlBody(
                    resetPasswordUrl,
                    login != null ? login : "",
                    locale != null ? locale : "ru"
            );

            sendEmail(senderEmail, targetEmail, subject, htmlBody);

            log.info("Reset password email sent successfully to: {}", targetEmail);
        } catch (Exception e) {
            log.error("Failed to send reset password email to: {}", targetEmail, e);
            throw new EmailSendException("Failed to send reset password email", e);
        }
    }

    /**
     * Отправка сообщения в техподдержку
     * Go: func (a *AuthService) SendMessageToSupport(...)
     *
     * @param userName Имя отправителя
     * @param email Email отправителя
     * @param message Текст сообщения
     * @param targetSubject Тип обращения
     */
    public void sendMessageToSupport(String userName, String email,
                                     String message, String targetSubject) {
        log.debug("Preparing support message from: {} ({})", userName, email);

        // Валидация
        validateNotEmpty(userName, "userName");
        validateEmail(email, "email");
        validateMessage(message);

        // Определение целевого email и темы
        // Go: switch target_subject
        EmailTarget target = determineEmailTarget(targetSubject);

        try {
            String htmlBody = getSupportMessageHtmlBody(userName, email, message);
            sendEmail(senderEmail, target.email, target.subject, htmlBody);

            log.info("Support message sent successfully from: {} to: {}",
                    email, target.email);
        } catch (Exception e) {
            log.error("Failed to send support message from: {}", email, e);
            throw new EmailSendException("Failed to send support message", e);
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Универсальный метод отправки email
     * Go: func SendingMessage(...)
     *
     * @param from    Email отправителя
     * @param to      Email получателя
     * @param subject Тема письма
     * @param body    Тело письма
     */
    private void sendEmail(String from, String to, String subject,
                           String body) {

        // Если включен режим отправки в логи (для dev/test)
        if (sendToLogs) {
            log.info("=== EMAIL (LOGS MODE) ===");
            log.info("From: {}", from);
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Body: {}", body);
            log.info("========================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

            log.debug("Email sent successfully: from={}, to={}, subject={}",
                    from, to, subject);
        } catch (Exception e) {
            log.error("Error sending email: from={}, to={}, subject={}",
                    from, to, subject, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }

    /**
     * Получение HTML тела для восстановления пароля
     * Go: func GetBodyMessageForResetPassword(...)
     *
     * Загружает шаблон из classpath и заменяет плейсхолдеры
     */
    private String getResetPasswordHtmlBody(String resetPasswordUrl,
                                            String login, String locale) {
        try {
            // Определяем путь к шаблону на основе локали
            String templatePath = "en".equalsIgnoreCase(locale)
                    ? "classpath:templates/mail/en/reset-password.html"
                    : "classpath:templates/mail/ru/reset-password.html";

            log.debug("Loading email template: {}", templatePath);

            // Загружаем шаблон из classpath
            Resource resource = resourceLoader.getResource(templatePath);
            if (!resource.exists()) {
                log.error("Email template not found: {}", templatePath);
                throw new EmailTemplateException("Email template not found: " + templatePath);
            }

            String template = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            // Замена плейсхолдеров (как в Go шаблонах)
            // Go: {{.UserName}}, {{.Login}}, {{.ResetPasswordUrl}}
            template = template.replace("{{.Login}}", escapeHtml(login));
            template = template.replace("{{.ResetPasswordUrl}}", resetPasswordUrl);

            return template;
        } catch (IOException e) {
            log.error("Error loading email template for locale: {}", locale, e);
            throw new EmailTemplateException("Failed to load email template", e);
        }
    }

    /**
     * Получение HTML тела для сообщения в поддержку
     * Go: func GetBodyMessageForSendMessageToSupport(...)
     */
    private String getSupportMessageHtmlBody(String userName, String email, String message) {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset=\"UTF-8\"></head>" +
                "<body style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                "<h2 style=\"color: #333;\">Обращение в техподдержку</h2>" +
                "<div style=\"background: #f5f5f5; padding: 15px; border-radius: 5px;\">" +
                "<p><strong>Имя:</strong> " + escapeHtml(userName) + "</p>" +
                "<p><strong>Email:</strong> " + escapeHtml(email) + "</p>" +
                "<p><strong>Сообщение:</strong></p>" +
                "<div style=\"background: white; padding: 10px; border-left: 3px solid #007bff;\">" +
                escapeHtml(message).replace("\n", "<br>") +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
        return html;
    }

    /**
     * Определение целевого email и темы на основе типа обращения
     * Go: switch target_subject в internal.go
     */
    private EmailTarget determineEmailTarget(String targetSubject) {
        if (targetSubject == null) {
            targetSubject = "other-help";
        }

        return switch (targetSubject) {
            case "features-consult" -> new EmailTarget(
                    productEmail,
                    "REPEAT: Консультация по возможностям продукта"
            );
            case "tariff-consult" -> new EmailTarget(
                    productEmail,
                    "REPEAT: Консультация по тарифам и приобретению продукта"
            );
            case "auth-help" -> new EmailTarget(
                    supportEmail,
                    "REPEAT: Требуется помощь с авторизацией/регистрацией"
            );
            case "other-help" -> new EmailTarget(
                    supportEmail,
                    "REPEAT: Другой вопрос"
            );
            default -> new EmailTarget(
                    supportEmail,
                    "REPEAT: Обращение в тех.поддержку"
            );
        };
    }

    /**
     * Валидация email адреса
     */
    private void validateEmail(String email, String fieldName) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        // Простая валидация email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format: " + fieldName);
        }
    }

    /**
     * Валидация непустой строки
     */
    private void validateNotEmpty(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }

    /**
     * Валидация сообщения
     * Go: validationMessage в validations.go
     */
    private void validateMessage(String message) {
        validateNotEmpty(message, "message");

        // Go: if len(message) > 4096
        if (message.length() > 4096) {
            throw new IllegalArgumentException(
                    "Message is too long (max 4096 characters)"
            );
        }
    }

    /**
     * Экранирование HTML для предотвращения XSS
     * Важно для безопасности при вставке пользовательских данных
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    // ==================== INNER CLASSES ====================

    /**
     * DTO для хранения информации о целевом email и теме
     */
    private record EmailTarget(String email, String subject) {}

    // ==================== CUSTOM EXCEPTIONS ====================

    /**
     * Исключение при ошибке отправки email
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Исключение при ошибке загрузки шаблона
     */
    public static class EmailTemplateException extends RuntimeException {
        public EmailTemplateException(String message) {
            super(message);
        }

        public EmailTemplateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}