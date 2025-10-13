package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Check;
import ru.platik777.backauth.entity.types.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сущность прав доступа пользователя к элементам системы
 *
 * ГИБРИДНЫЙ ПОДХОД:
 * - В БД хранится битовая маска (2 байта, быстро)
 * - В API отдается массив строк (читаемо)
 *
 * Битовая маска permissions (0-7):
 * - 0 (000) - нет доступа
 * - 1 (001) - READ
 * - 2 (010) - WRITE (невозможен без READ) - НЕВАЛИДНО
 * - 3 (011) - READ + WRITE
 * - 4 (100) - EXECUTE (невозможен без READ) - НЕВАЛИДНО
 * - 5 (101) - READ + EXECUTE
 * - 6 (110) - WRITE + EXECUTE (невозможен без READ) - НЕВАЛИДНО
 * - 7 (111) - READ + WRITE + EXECUTE
 *
 * ВАЖНО: Если permission != 0, то бит READ обязателен
 */
@Entity
@Table(
        name = "item_user_permission",
        indexes = {
                @Index(name = "idx_permission_user", columnList = "user_id"),
                @Index(name = "idx_permission_user_project", columnList = "user_id, project_id"),
                @Index(name = "idx_permission_user_folder", columnList = "user_id, folder_id"),
                @Index(name = "idx_permission_user_file", columnList = "user_id, file_id"),
                @Index(name = "idx_permission_user_block", columnList = "user_id, block_id")
        }
)
@Check(constraints = """
    (project_id IS NOT NULL AND folder_id IS NULL AND file_id IS NULL AND block_id IS NULL) OR
    (project_id IS NULL AND folder_id IS NOT NULL AND file_id IS NULL AND block_id IS NULL) OR
    (project_id IS NULL AND folder_id IS NULL AND file_id IS NOT NULL AND block_id IS NULL) OR
    (project_id IS NULL AND folder_id IS NULL AND file_id IS NULL AND block_id IS NOT NULL)
    """)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ItemUserPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Битовая маска прав доступа (0-7)
     * Хранится в БД для производительности
     *
     * Валидные значения:
     * - 0: нет доступа
     * - 1: READ (001)
     * - 3: READ + WRITE (011)
     * - 5: READ + EXECUTE (101)
     * - 7: READ + WRITE + EXECUTE (111)
     *
     * Невалидные значения (нет READ при наличии других прав):
     * - 2: WRITE без READ (010)
     * - 4: EXECUTE без READ (100)
     * - 6: WRITE + EXECUTE без READ (110)
     */
    @Column(nullable = false)
    private Short permissions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    // ==================== ГИБРИДНЫЙ ПОДХОД ====================

    /**
     * Получить список прав в виде строк (для API)
     * Не хранится в БД, вычисляется на лету
     *
     * @return список прав: ["READ", "WRITE", "EXECUTE"]
     */
    @Transient
    public List<String> getPermissionsList() {
        List<String> result = new ArrayList<>();

        if (permissions == null || permissions == 0) {
            return result;
        }

        if (Permission.hasPermission(permissions, Permission.READ)) {
            result.add("READ");
        }
        if (Permission.hasPermission(permissions, Permission.WRITE)) {
            result.add("WRITE");
        }
        if (Permission.hasPermission(permissions, Permission.EXECUTE)) {
            result.add("EXECUTE");
        }

        return result;
    }

    /**
     * Установить права из списка строк (для API)
     * Конвертирует ["READ", "WRITE"] в битовую маску
     *
     * @param permissionsList список прав
     */
    @Transient
    public void setPermissionsList(List<String> permissionsList) {
        if (permissionsList == null || permissionsList.isEmpty()) {
            this.permissions = 0;
            return;
        }

        short mask = 0;

        for (String perm : permissionsList) {
            switch (perm.toUpperCase()) {
                case "READ" -> mask |= Permission.READ.getValue();
                case "WRITE" -> mask |= Permission.WRITE.getValue();
                case "EXECUTE" -> mask |= Permission.EXECUTE.getValue();
                default -> throw new IllegalArgumentException("Unknown permission: " + perm);
            }
        }

        this.permissions = mask;
    }

    /**
     * Проверить наличие конкретного права
     *
     * @param permission право для проверки
     * @return true если право есть
     */
    @Transient
    public boolean hasPermission(Permission permission) {
        return Permission.hasPermission(this.permissions, permission);
    }

    /**
     * Проверить наличие всех указанных прав
     *
     * @param requiredPermissions требуемые права
     * @return true если все права есть
     */
    @Transient
    public boolean hasAllPermissions(Permission... requiredPermissions) {
        return Permission.hasAllPermissions(this.permissions, requiredPermissions);
    }

    /**
     * Проверить наличие хотя бы одного из указанных прав
     *
     * @param requiredPermissions требуемые права
     * @return true если хотя бы одно право есть
     */
    @Transient
    public boolean hasAnyPermission(Permission... requiredPermissions) {
        return Permission.hasAnyPermission(this.permissions, requiredPermissions);
    }

    /**
     * Добавить право
     *
     * @param permission право для добавления
     */
    @Transient
    public void addPermission(Permission permission) {
        this.permissions = (short) (this.permissions | permission.getValue());
    }

    /**
     * Удалить право
     *
     * @param permission право для удаления
     */
    @Transient
    public void removePermission(Permission permission) {
        this.permissions = (short) (this.permissions & ~permission.getValue());
    }

    /**
     * Человекочитаемое описание прав
     *
     * @return строка вида "READ + WRITE + EXECUTE"
     */
    @Transient
    public String getPermissionsDescription() {
        if (permissions == null || permissions == 0) {
            return "No access";
        }

        return String.join(" + ", getPermissionsList());
    }

    // ==================== LEGACY METHODS ====================

    /**
     * Получить UUID элемента, к которому привязано разрешение
     */
    public UUID getItemId() {
        if (project != null) return project.getId();
        if (folder != null) return folder.getId();
        if (file != null) return file.getId();
        if (block != null) return block.getId();
        return null;
    }

    /**
     * Получить тип элемента
     */
    public String getItemType() {
        if (project != null) return "PROJECT";
        if (folder != null) return "FOLDER";
        if (file != null) return "FILE";
        if (block != null) return "BLOCK";
        return null;
    }

    // ==================== VALIDATION ====================

    /**
     * Валидация permissions перед сохранением
     * Проверяет что если есть WRITE или EXECUTE, то обязательно есть READ
     */
    @PrePersist
    @PreUpdate
    public void validatePermissions() {
        if (permissions == null) {
            throw new IllegalStateException("Permissions cannot be null");
        }

        if (permissions < 0 || permissions > 7) {
            throw new IllegalStateException(
                    "Permissions must be between 0 and 7, got: " + permissions
            );
        }

        // Проверка: если permission != 0, то должен быть бит READ (001)
        if (permissions != 0 && (permissions & 1) == 0) {
            throw new IllegalStateException(
                    "Invalid permissions value: " + permissions +
                            " (" + Integer.toBinaryString(permissions) + "). " +
                            "WRITE or EXECUTE cannot exist without READ permission. " +
                            "Valid values: 0, 1 (READ), 3 (READ+WRITE), 5 (READ+EXECUTE), 7 (FULL)"
            );
        }

        // Проверка что заполнен ровно один элемент
        int filledItems = 0;
        if (project != null) filledItems++;
        if (folder != null) filledItems++;
        if (file != null) filledItems++;
        if (block != null) filledItems++;

        if (filledItems != 1) {
            throw new IllegalStateException(
                    "Exactly one item (project/folder/file/block) must be set, got: " + filledItems
            );
        }
    }
}