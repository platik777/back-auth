package ru.platik777.backauth.entity.types;

import lombok.Getter;

/**
 * Enum для прав доступа с битовыми масками
 *
 * Используется гибридный подход:
 * - В БД хранится битовая маска (производительность)
 * - В коде используется enum (читаемость)
 * - В API отдается массив строк (удобство)
 *
 * Битовые маски:
 * - READ:    001 = 1
 * - WRITE:   010 = 2
 * - EXECUTE: 100 = 4
 */
@Getter
public enum Permission {
    READ(1),    // 001 = 1
    WRITE(2),   // 010 = 2
    EXECUTE(4); // 100 = 4

    private final int value;

    Permission(int value) {
        this.value = value;
    }

    // ==================== ПРЕДОПРЕДЕЛЕННЫЕ КОМБИНАЦИИ ====================

    /**
     * READ + WRITE = 3 (011)
     * Самая частая комбинация для редактирования
     */
    public static final int READ_WRITE = 3;

    /**
     * READ + EXECUTE = 5 (101)
     * Для выполнения без возможности изменения
     */
    public static final int READ_EXECUTE = 5;

    /**
     * READ + WRITE + EXECUTE = 7 (111)
     * Полный доступ
     */
    public static final int FULL = 7;

    /**
     * Нет доступа = 0 (000)
     */
    public static final int NONE = 0;

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С БИТОВЫМИ МАСКАМИ ====================

    /**
     * Объединить несколько прав в битовую маску
     *
     * @param permissions права для объединения
     * @return битовая маска
     *
     * @example Permission.combine(Permission.READ, Permission.WRITE) → 3
     */
    public static int combine(Permission... permissions) {
        int result = 0;
        for (Permission permission : permissions) {
            result |= permission.value;
        }
        return result;
    }

    /**
     * Проверить наличие конкретного права
     *
     * @param permissions битовая маска прав
     * @param permission право для проверки
     * @return true если право есть
     *
     * @example Permission.hasPermission(3, Permission.READ) → true
     * @example Permission.hasPermission(3, Permission.EXECUTE) → false
     */
    public static boolean hasPermission(int permissions, Permission permission) {
        return (permissions & permission.value) == permission.value;
    }

    /**
     * Проверить наличие ВСЕХ указанных прав
     *
     * @param permissions битовая маска прав
     * @param required требуемые права
     * @return true если все права есть
     *
     * @example Permission.hasAllPermissions(7, Permission.READ, Permission.WRITE) → true
     * @example Permission.hasAllPermissions(1, Permission.READ, Permission.WRITE) → false
     */
    public static boolean hasAllPermissions(int permissions, Permission... required) {
        int requiredMask = combine(required);
        return (permissions & requiredMask) == requiredMask;
    }

    /**
     * Проверить наличие ВСЕХ прав по битовой маске
     *
     * @param permissions битовая маска прав
     * @param requiredMask требуемая битовая маска
     * @return true если все биты установлены
     *
     * @example Permission.hasAllPermissions(7, 3) → true (есть READ+WRITE)
     * @example Permission.hasAllPermissions(1, 3) → false (нет WRITE)
     */
    public static boolean hasAllPermissions(int permissions, int requiredMask) {
        return (permissions & requiredMask) == requiredMask;
    }

    /**
     * Проверить наличие ХОТЯ БЫ ОДНОГО из указанных прав
     *
     * @param permissions битовая маска прав
     * @param required требуемые права
     * @return true если хотя бы одно право есть
     *
     * @example Permission.hasAnyPermission(1, Permission.READ, Permission.WRITE) → true (есть READ)
     * @example Permission.hasAnyPermission(1, Permission.WRITE, Permission.EXECUTE) → false
     */
    public static boolean hasAnyPermission(int permissions, Permission... required) {
        int requiredMask = combine(required);
        return (permissions & requiredMask) != 0;
    }

    /**
     * Проверить наличие ХОТЯ БЫ ОДНОГО бита из маски
     *
     * @param permissions битовая маска прав
     * @param requiredMask требуемая битовая маска
     * @return true если хотя бы один бит установлен
     *
     * @example Permission.hasAnyPermission(1, 3) → true (есть READ из READ+WRITE)
     * @example Permission.hasAnyPermission(1, 6) → false (нет ни WRITE, ни EXECUTE)
     */
    public static boolean hasAnyPermission(int permissions, int requiredMask) {
        return (permissions & requiredMask) != 0;
    }

    // ==================== ВАЛИДАЦИЯ ====================

    /**
     * Проверить валидность битовой маски
     *
     * Правила:
     * - Значение должно быть от 0 до 7
     * - Если есть WRITE или EXECUTE, то обязательно должен быть READ
     *
     * @param permissions битовая маска для проверки
     * @return true если маска валидна
     *
     * @example Permission.isValid(3) → true  (READ + WRITE)
     * @example Permission.isValid(2) → false (WRITE без READ)
     * @example Permission.isValid(8) → false (вне диапазона)
     */
    public static boolean isValid(int permissions) {
        // Проверка диапазона
        if (permissions < 0 || permissions > 7) {
            return false;
        }

        // Проверка: если не 0, то должен быть READ
        if (permissions != 0 && (permissions & 1) == 0) {
            return false;
        }

        return true;
    }

    /**
     * Валидировать битовую маску с выбросом исключения
     *
     * @param permissions битовая маска для проверки
     * @throws IllegalArgumentException если маска невалидна
     */
    public static void validate(int permissions) {
        if (!isValid(permissions)) {
            String binary = Integer.toBinaryString(permissions);
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid permissions value: %d (%s). " +
                                    "Valid values: 0 (NONE), 1 (READ), 3 (READ+WRITE), " +
                                    "5 (READ+EXECUTE), 7 (FULL). " +
                                    "WRITE or EXECUTE cannot exist without READ.",
                            permissions,
                            binary
                    )
            );
        }
    }

    // ==================== УТИЛИТЫ ====================

    /**
     * Получить описание прав в читаемом виде
     *
     * @param permissions битовая маска
     * @return строка вида "READ + WRITE + EXECUTE"
     *
     * @example Permission.describe(7) → "READ + WRITE + EXECUTE"
     * @example Permission.describe(1) → "READ"
     * @example Permission.describe(0) → "No access"
     */
    public static String describe(int permissions) {
        if (permissions == 0) {
            return "No access";
        }

        StringBuilder result = new StringBuilder();

        if (hasPermission(permissions, READ)) {
            result.append("READ");
        }

        if (hasPermission(permissions, WRITE)) {
            if (!result.isEmpty()) result.append(" + ");
            result.append("WRITE");
        }

        if (hasPermission(permissions, EXECUTE)) {
            if (!result.isEmpty()) result.append(" + ");
            result.append("EXECUTE");
        }

        return result.toString();
    }

    /**
     * Распарсить строку в битовую маску
     *
     * @param permissionString строка вида "READ,WRITE" или "READ+WRITE"
     * @return битовая маска
     * @throws IllegalArgumentException если строка невалидна
     *
     * @example Permission.parse("READ,WRITE") → 3
     * @example Permission.parse("READ+WRITE+EXECUTE") → 7
     */
    public static int parse(String permissionString) {
        if (permissionString == null || permissionString.trim().isEmpty()) {
            return NONE;
        }

        String[] parts = permissionString
                .toUpperCase()
                .replaceAll("\\s+", "")
                .split("[,+]");

        int result = 0;

        for (String part : parts) {
            switch (part) {
                case "READ" -> result |= READ.value;
                case "WRITE" -> result |= WRITE.value;
                case "EXECUTE" -> result |= EXECUTE.value;
                case "NONE" -> { /* ничего не делаем */ }
                default -> throw new IllegalArgumentException(
                        "Unknown permission: " + part +
                                ". Valid values: READ, WRITE, EXECUTE, NONE"
                );
            }
        }

        validate(result); // Проверяем валидность результата
        return result;
    }

    /**
     * Добавить право к существующей маске
     *
     * @param current текущая битовая маска
     * @param permission право для добавления
     * @return новая битовая маска
     *
     * @example Permission.add(1, Permission.WRITE) → 3 (READ + WRITE)
     */
    public static int add(int current, Permission permission) {
        return current | permission.value;
    }

    /**
     * Удалить право из существующей маски
     *
     * @param current текущая битовая маска
     * @param permission право для удаления
     * @return новая битовая маска
     *
     * @example Permission.remove(7, Permission.WRITE) → 5 (READ + EXECUTE)
     */
    public static int remove(int current, Permission permission) {
        return current & ~permission.value;
    }
}