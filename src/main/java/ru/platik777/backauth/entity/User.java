package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сущность пользователя - соответствует models.User из Go
 *
 * ВАЖНО: Эта entity хранится в public.users, но содержит также transient поля
 * из userN.user_data, которые заполняются вручную после получения из БД.
 *
 * Архитектура БД:
 * - public.users: user_id, login, account_type, settings, billing_id, created_at
 * - userN.user_data: password_hash, email, user_name, phone (отдельная схема!)
 *
 * В Go эта структура заполняется из обеих таблиц методом GetUser.
 */
@Entity
@Table(name = "users", schema = "public")
@Data
@EqualsAndHashCode(callSuper = false)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id; // TODO: Перейти на UUID

    @Column(name = "login", unique = true, nullable = false, length = 255)
    private String login;

    @Column(name = "billing_id")
    private Integer billingId = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType accountType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private UserSettings settings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ApiKey> apiKeys;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Company> ownedCompanies;

    // ========== TRANSIENT ПОЛЯ ИЗ userN.user_data ==========
    // Эти поля НЕ хранятся в public.users, а заполняются вручную
    // из таблицы userN.user_data после получения пользователя

    @Transient
    private String passwordHash;

    @Transient
    private String email;

    @Transient
    private String userName;

    @Transient
    private String phone;

    // ========== TRANSIENT ПОЛЯ ИЗ SDK ==========
    // Получаются через getUserRolePermission

    @Transient
    private List<String> roles;

    // TODO: по идее избавится от этого, так как есть таблица item_user_permission
    @Transient
    private List<String> permissions;

    @Getter
    public enum AccountType {
        INDIVIDUAL("individual"),
        STUDENT("student"),
        ENTREPRENEUR("entrepreneur"),
        COMPANY_RESIDENT("company_resident"),
        COMPANY_NON_RESIDENT("company_non_resident"),
        EDUCATIONAL_UNIT("educational_unit");

        private final String value;

        AccountType(String value) {
            this.value = value;
        }

        public static AccountType fromValue(String value) {
            for (AccountType type : AccountType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown account type: " + value);
        }
    }

    @Data
    public static class UserSettings {
        private String locale = "ru";
        private String theme;
        private Boolean isEngineer = false;
        private Object iconType;
    }

    /**
     * Lifecycle callback - выполняется перед сохранением
     */
    @PrePersist
    protected void onCreate() {
        if (settings == null) {
            settings = new UserSettings();
        }
        if (billingId == null) {
            billingId = 0;
        }
    }
}