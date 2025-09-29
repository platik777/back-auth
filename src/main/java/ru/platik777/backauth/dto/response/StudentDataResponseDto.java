package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для данных студента
 * Соответствует ответу GetStudent из Go
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDataResponseDto {

    /**
     * ID записи студента
     */
    private Integer id;

    /**
     * Учебное заведение
     */
    private String educationalInstitution;

    /**
     * Курс обучения
     */
    private String course;

    /**
     * Группа
     */
    private String group;

    /**
     * Дата создания записи
     */
    private String createdAt;
}