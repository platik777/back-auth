package ru.platik777.backauth.entity.types;

import lombok.Getter;

@Getter
public enum Permission {
    READ(1),    // 001 = 1
    WRITE(2),   // 010 = 2
    EXECUTE(4); // 100 = 4

    private final int value;

    Permission(int value) {
        this.value = value;
    }

    public static int combine(Permission... permissions) {
        int result = 0;
        for (Permission permission : permissions) {
            result |= permission.value;
        }
        return result;
    }

    public static boolean hasPermission(int permissions, Permission permission) {
        return (permissions & permission.value) == permission.value;
    }

    public static boolean hasAllPermissions(int permissions, Permission... required) {
        int requiredMask = combine(required);
        return (permissions & requiredMask) == requiredMask;
    }

    public static boolean hasAllPermissions(int permissions, int requiredMask) {
        return (permissions & requiredMask) == requiredMask;
    }

    public static boolean hasAnyPermission(int permissions, Permission... required) {
        int requiredMask = combine(required);
        return (permissions & requiredMask) != 0;
    }

    // Дополнительный метод для работы с int маской
    public static boolean hasAnyPermission(int permissions, int requiredMask) {
        return (permissions & requiredMask) != 0;
    }
}