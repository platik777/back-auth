package ru.platik777.backauth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.types.ItemType;

/**
 * Запрос на отзыв прав доступа у пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokePermissionRequest {
    /**
     * ID пользователя, у которого отзываются права
     */
    private String targetUserId;

    /**
     * ID элемента
     */
    private String itemId;

    /**
     * Тип элемента
     */
    private ItemType itemType;
}