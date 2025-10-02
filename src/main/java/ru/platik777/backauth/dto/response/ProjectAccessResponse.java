package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на проверку принадлежности проекта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAccessResponse {
    private Boolean hasAccess;
}
