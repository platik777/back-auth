package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для ответа со всеми доступными элементами пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessibleItemsResponse {
    /**
     * ID пользователя
     */
    private String userId;

    /**
     * Доступные проекты
     */
    private List<ItemPermissionResponse> projects;

    /**
     * Доступные папки
     */
    private List<ItemPermissionResponse> folders;

    /**
     * Доступные файлы
     */
    private List<ItemPermissionResponse> files;

    /**
     * Доступные блоки
     */
    private List<ItemPermissionResponse> blocks;

    /**
     * Общее количество доступных элементов
     */
    private Integer totalItems;

    /**
     * Вычислить общее количество элементов
     */
    public void calculateTotalItems() {
        this.totalItems =
                (projects != null ? projects.size() : 0) +
                        (folders != null ? folders.size() : 0) +
                        (files != null ? files.size() : 0) +
                        (blocks != null ? blocks.size() : 0);
    }
}