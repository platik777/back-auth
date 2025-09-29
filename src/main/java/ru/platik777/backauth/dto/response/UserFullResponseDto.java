package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для полных данных пользователя с ролями и правами
 * Соответствует ответу GetUser из Go
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFullResponseDto {

    /**
     * ID пользователя
     */
    private Integer id;

    /**
     * Логин
     */
    private String login;

    /**
     * Имя пользователя
     */
    private String userName;

    /**
     * Email
     */
    private String email;

    /**
     * Телефон
     */
    private String phone;

    /**
     * Локаль (ru, en)
     */
    private String locale;

    /**
     * Роли пользователя
     */
    private List<RoleDto> roles;

    /**
     * Права доступа пользователя
     */
    private List<AccessDto> access;

    /**
     * Дата создания
     */
    private String createdAt;

    /**
     * Данные студента (если есть)
     */
    private StudentDataDto student;

    /**
     * Данные компании (если есть)
     */
    private List<CompanyDataDto> companies;

    /**
     * DTO для ролей
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDto {
        private Integer id;
        private String name;
        private String description;
    }

    /**
     * DTO для прав доступа
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessDto {
        private Integer id;
        private String moduleName;
        private Boolean canView;
        private Boolean canEdit;
        private Boolean canDelete;
    }

    /**
     * DTO для данных студента
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentDataDto {
        private String educationalInstitution;
        private String course;
        private String group;
    }

    /**
     * DTO для данных компании
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyDataDto {
        private String name;
        private String inn;
        private String position;
    }
}