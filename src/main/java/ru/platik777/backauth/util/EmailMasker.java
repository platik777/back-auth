package ru.platik777.backauth.util;

import org.springframework.stereotype.Component;

/**
 * Утилита для маскирования чувствительных данных в логах
 */
@Component
public class EmailMasker {

    /**
     * Маскирование email для логирования
     * Пример: john.doe@example.com -> j***e@e***e.com
     *
     * @param email email для маскирования
     * @return замаскированный email
     */
    public String mask(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        String maskedLocal = maskPart(localPart);
        String maskedDomain = maskPart(domain);

        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * Маскирование части строки (локальная часть или домен)
     */
    private String maskPart(String part) {
        if (part.length() <= 2) {
            return "***";
        }
        return part.charAt(0) + "***" + part.charAt(part.length() - 1);
    }

    /**
     * Маскирование телефона
     * Пример: +7 916 1234567890 -> +7 *** ********90
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }

        // Показываем только первые 2 и последние 2 символа
        return phone.substring(0, 2) + " *** " +
                "*".repeat(Math.max(0, phone.length() - 6)) +
                phone.substring(phone.length() - 2);
    }
}