package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для списка компаний пользователя
 * Соответствует ответу GetCompanies из Go
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyListResponseDto {

    /**
     * Список компаний
     */
    private List<CompanyItemDto> companies;

    /**
     * DTO для отдельной компании
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyItemDto {
        /**
         * ID компании
         */
        private Integer id;

        /**
         * Название компании
         */
        private String name;

        /**
         * ИНН компании
         */
        private String inn;

        /**
         * Должность в компании
         */
        private String position;

        /**
         * Дата создания записи
         */
        private String createdAt;
    }
}