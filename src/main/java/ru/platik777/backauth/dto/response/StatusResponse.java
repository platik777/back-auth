package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Общий ответ с статусом
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusResponse {
    private Boolean status;
    private String message;
}
