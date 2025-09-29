package ru.platik777.backauth.service;

import ru.platik777.backauth.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Сервис для отправки email уведомлений
 * Реализует логику из Go версии smtp пакета
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    /**
     * Отправка email для восстановления пароля (аналог GetBodyMessageForResetPassword из Go)
     */
    public void sendPasswordResetEmail(String targetEmail, String userName, String login, String resetToken, String locale) {
        log.debug("Sending password reset email to: {}", targetEmail);

        try {
            // Проверяем настройку отправки в логи
            if (appProperties.getSmtp().isSendToLogs()) {
                sendPasswordResetToLogs(targetEmail, userName, login, resetToken, locale);
                return;
            }

            // Создание URL для сброса пароля
            String resetPasswordUrl = appProperties.getBaseUrl() + "/auth/forgot-password/update/?token=" + resetToken;

            // Получение шаблона email
            String htmlContent = getPasswordResetTemplate(locale);

            // Замена плейсхолдеров
            htmlContent = htmlContent.replace("{baseUrl}", appProperties.getBaseUrl());
            htmlContent = htmlContent.replace("{userName}", userName);
            htmlContent = htmlContent.replace("{resetPasswordUrl}", resetPasswordUrl);
            htmlContent = htmlContent.replace("{login}", login);

            // Создание и отправка сообщения
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(targetEmail);
            helper.setSubject(getPasswordResetSubject(locale));
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Password reset email sent successfully to: {}", targetEmail);

        } catch (Exception e) {
            log.error("Error sending password reset email to: {}", targetEmail, e);
            throw new RuntimeException("Error sending email", e);
        }
    }

    /**
     * Отправка сообщения в техподдержку (аналог GetBodyMessageForSendMessageToSupport из Go)
     */
    public void sendSupportMessage(String senderName, String senderEmail, String message, String subject) {
        log.debug("Sending support message from: {}", senderEmail);

        try {
            // Определение email получателя в зависимости от темы
            String targetEmail = getSupportEmailBySubject(subject);
            String emailSubject = getSupportSubject(subject);

            // Формирование тела сообщения
            String fullMessage = String.format("%s\r\n%s\r\n\r\n%s", senderName, senderEmail, message);

            // Создание и отправка сообщения
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(targetEmail);
            helper.setSubject(emailSubject);
            helper.setText(fullMessage, false);

            mailSender.send(mimeMessage);

            log.info("Support message sent successfully from: {} to: {}", senderEmail, targetEmail);

        } catch (Exception e) {
            log.error("Error sending support message from: {}", senderEmail, e);
            throw new RuntimeException("Error sending support message", e);
        }
    }

    /**
     * Отправка письма в логи (для тестовых контуров)
     */
    private void sendPasswordResetToLogs(String targetEmail, String userName, String login, String resetToken, String locale) {
        String resetPasswordUrl = appProperties.getBaseUrl() + "/auth/forgot-password/update/?token=" + resetToken;

        log.info("===== EMAIL TO LOGS =====");
        log.info("To: {}", targetEmail);
        log.info("Subject: {}", getPasswordResetSubject(locale));
        log.info("Username: {}", userName);
        log.info("Login: {}", login);
        log.info("Reset URL: {}", resetPasswordUrl);
        log.info("Locale: {}", locale);
        log.info("========================");
    }

    /**
     * Получение шаблона для восстановления пароля
     */
    private String getPasswordResetTemplate(String locale) throws IOException {
        String templatePath = "ru".equals(locale)
                ? "mail-templates/ru/reset-password.html"
                : "mail-templates/en/reset-password.html";

        ClassPathResource resource = new ClassPathResource(templatePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Получение темы письма для восстановления пароля
     */
    private String getPasswordResetSubject(String locale) {
        return "en".equals(locale)
                ? "REPEAT: Password Recovery"
                : "REPEAT: Восстановление пароля";
    }

    /**
     * Получение email получателя по теме обращения
     */
    private String getSupportEmailBySubject(String subject) {
        return switch (subject) {
            case "features-consult", "tariff-consult" -> appProperties.getSmtp().getProductEmail();
            case "auth-help", "other-help" -> appProperties.getSmtp().getSupportEmail();
            default -> appProperties.getSmtp().getSupportEmail();
        };
    }

    /**
     * Получение темы письма для техподдержки
     */
    private String getSupportSubject(String subject) {
        return switch (subject) {
            case "features-consult" -> "REPEAT: Консультация по возможностям продукта";
            case "tariff-consult" -> "REPEAT: Консультация по тарифам и приобретению продукта";
            case "auth-help" -> "REPEAT: Требуется помощь с авторизацией/регистрацией";
            case "other-help" -> "REPEAT: Другой вопрос";
            default -> "REPEAT: Обращение в тех.поддержку";
        };
    }

    /**
     * Проверка настроек SMTP
     */
    public boolean isEmailServiceEnabled() {
        // Проверяем, настроен ли SMTP
        return mailSender != null;
    }

    /**
     * Тестовая отправка email
     */
    public void sendTestEmail(String targetEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(targetEmail);
            helper.setSubject("REPEAT: Test Email");
            helper.setText("This is a test email from REPEAT Auth Service", false);

            mailSender.send(message);

            log.info("Test email sent successfully to: {}", targetEmail);

        } catch (Exception e) {
            log.error("Error sending test email to: {}", targetEmail, e);
            throw new RuntimeException("Error sending test email", e);
        }
    }
}