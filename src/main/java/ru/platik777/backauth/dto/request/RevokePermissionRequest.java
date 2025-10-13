package ru.platik777.backauth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.types.ItemType;

import java.util.UUID;

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
    private UUID targetUserId;

    /**
     * ID элемента
     */
    private UUID itemId;

    /**
     * Тип элемента
     */
    private ItemType itemType;
}