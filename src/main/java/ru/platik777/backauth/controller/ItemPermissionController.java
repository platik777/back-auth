package ru.platik777.backauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.backauth.dto.request.*;
import ru.platik777.backauth.dto.response.*;
import ru.platik777.backauth.entity.ItemUserPermission;
import ru.platik777.backauth.entity.types.ItemType;
import ru.platik777.backauth.entity.types.Permission;
import ru.platik777.backauth.security.CurrentUser;
import ru.platik777.backauth.service.ItemPermissionService;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер управления правами доступа к элементам системы
 *
 * Базовый путь: /api/v1/permissions
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Permissions", description = "Управление правами доступа к элементам")
public class ItemPermissionController {

    private final ItemPermissionService permissionService;

    /**
     * POST /api/v1/permissions/grant
     * Выдать права доступа пользователю
     */
    @PostMapping("/grant")
    @Operation(summary = "Выдать права доступа",
            description = "Выдает права доступа пользователю на элемент. Требует наличия прав WRITE у текущего пользователя.")
    public ResponseEntity<ItemPermissionResponse> grantPermission(
            @CurrentUser @Parameter(hidden = true) UUID granterId,
            @RequestBody GrantPermissionRequest request) {

        log.info("Grant permission request: granter={}, target={}, item={}, type={}",
                granterId, request.getTargetUserId(), request.getItemId(), request.getItemType());

        // Определяем права из запроса (битовая маска или список)
        short permissions;
        if (request.getPermissions() != null) {
            permissions = request.getPermissions();
        } else if (request.getPermissionsList() != null && !request.getPermissionsList().isEmpty()) {
            permissions = (short) Permission.parse(String.join(",", request.getPermissionsList()));
        } else {
            throw new IllegalArgumentException("Either permissions or permissionsList must be provided");
        }

        // Выдаем права
        ItemUserPermission granted = permissionService.grantPermission(
                granterId,
                request.getTargetUserId(),
                request.getItemId(),
                request.getItemType(),
                permissions
        );

        ItemPermissionResponse response = ItemPermissionResponse.fromEntity(granted);

        log.info("Permission granted successfully: id={}, target={}, item={}",
                granted.getId(), request.getTargetUserId(), request.getItemId());

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/permissions/update
     * Обновить права доступа пользователя
     */
    @PutMapping("/update")
    @Operation(summary = "Обновить права доступа",
            description = "Обновляет существующие права доступа пользователя на элемент. Требует наличия прав WRITE.")
    public ResponseEntity<ItemPermissionResponse> updatePermission(
            @CurrentUser @Parameter(hidden = true) UUID updaterId,
            @RequestBody UpdatePermissionRequest request) {

        log.info("Update permission request: updater={}, target={}, item={}, type={}",
                updaterId, request.getTargetUserId(), request.getItemId(), request.getItemType());

        // Определяем новые права
        short newPermissions;
        if (request.getPermissions() != null) {
            newPermissions = request.getPermissions();
        } else if (request.getPermissionsList() != null && !request.getPermissionsList().isEmpty()) {
            newPermissions = (short) Permission.parse(String.join(",", request.getPermissionsList()));
        } else {
            throw new IllegalArgumentException("Either permissions or permissionsList must be provided");
        }

        // Обновляем права
        ItemUserPermission updated = permissionService.updatePermission(
                updaterId,
                request.getTargetUserId(),
                request.getItemId(),
                request.getItemType(),
                newPermissions
        );

        ItemPermissionResponse response = ItemPermissionResponse.fromEntity(updated);

        log.info("Permission updated successfully: id={}, target={}, item={}",
                updated.getId(), request.getTargetUserId(), request.getItemId());

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/permissions/revoke
     * Отозвать права доступа у пользователя
     */
    @DeleteMapping("/revoke")
    @Operation(summary = "Отозвать права доступа",
            description = "Полностью отзывает права доступа пользователя к элементу. Требует наличия прав WRITE.")
    public ResponseEntity<StatusResponse> revokePermission(
            @CurrentUser @Parameter(hidden = true) UUID revokerId,
            @RequestBody RevokePermissionRequest request) {

        log.info("Revoke permission request: revoker={}, target={}, item={}, type={}",
                revokerId, request.getTargetUserId(), request.getItemId(), request.getItemType());

        // Отзываем права
        permissionService.revokePermission(
                revokerId,
                request.getTargetUserId(),
                request.getItemId(),
                request.getItemType()
        );

        log.info("Permission revoked successfully: target={}, item={}",
                request.getTargetUserId(), request.getItemId());

        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status(true)
                        .message("Permission revoked successfully")
                        .build()
        );
    }

    // ==================== ПОЛУЧЕНИЕ ДОСТУПНЫХ ЭЛЕМЕНТОВ ====================

    /**
     * GET /api/v1/permissions/accessible
     * Получить все доступные элементы пользователя
     */
    @GetMapping("/accessible")
    @Operation(summary = "Получить все доступные элементы",
            description = "Возвращает все проекты, папки, файлы и блоки, доступные пользователю на чтение")
    public ResponseEntity<UserAccessibleItemsResponse> getAccessibleItems(
            @CurrentUser @Parameter(hidden = true) UUID userId) {

        log.info("Getting accessible items for userId: {}", userId);

        // Получаем все доступные элементы
        List<ItemUserPermission> projects = permissionService.getReadableProjects(userId);
        List<ItemUserPermission> folders = permissionService.getReadableFolders(userId);
        List<ItemUserPermission> files = permissionService.getReadableFiles(userId);
        List<ItemUserPermission> blocks = permissionService.getReadableBlocks(userId);

        // Формируем ответ
        UserAccessibleItemsResponse response = UserAccessibleItemsResponse.builder()
                .userId(userId)
                .projects(ItemPermissionResponse.fromEntities(projects))
                .folders(ItemPermissionResponse.fromEntities(folders))
                .files(ItemPermissionResponse.fromEntities(files))
                .blocks(ItemPermissionResponse.fromEntities(blocks))
                .build();

        response.calculateTotalItems();

        log.info("Found {} accessible items for userId: {}", response.getTotalItems(), userId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/permissions/accessible/projects
     * Получить доступные проекты
     */
    @GetMapping("/accessible/projects")
    @Operation(summary = "Получить доступные проекты")
    public ResponseEntity<List<ItemPermissionResponse>> getAccessibleProjects(
            @CurrentUser @Parameter(hidden = true) UUID userId) {

        log.debug("Getting accessible projects for userId: {}", userId);

        List<ItemUserPermission> projects = permissionService.getReadableProjects(userId);
        List<ItemPermissionResponse> response = ItemPermissionResponse.fromEntities(projects);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/permissions/accessible/folders
     * Получить доступные папки
     */
    @GetMapping("/accessible/folders")
    @Operation(summary = "Получить доступные папки")
    public ResponseEntity<List<ItemPermissionResponse>> getAccessibleFolders(
            @CurrentUser @Parameter(hidden = true) UUID userId) {

        log.debug("Getting accessible folders for userId: {}", userId);

        List<ItemUserPermission> folders = permissionService.getReadableFolders(userId);
        List<ItemPermissionResponse> response = ItemPermissionResponse.fromEntities(folders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/permissions/accessible/files
     * Получить доступные файлы
     */
    @GetMapping("/accessible/files")
    @Operation(summary = "Получить доступные файлы")
    public ResponseEntity<List<ItemPermissionResponse>> getAccessibleFiles(
            @CurrentUser @Parameter(hidden = true) UUID userId) {

        log.debug("Getting accessible files for userId: {}", userId);

        List<ItemUserPermission> files = permissionService.getReadableFiles(userId);
        List<ItemPermissionResponse> response = ItemPermissionResponse.fromEntities(files);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/permissions/accessible/blocks
     * Получить доступные блоки
     */
    @GetMapping("/accessible/blocks")
    @Operation(summary = "Получить доступные блоки")
    public ResponseEntity<List<ItemPermissionResponse>> getAccessibleBlocks(
            @CurrentUser @Parameter(hidden = true) UUID userId) {

        log.debug("Getting accessible blocks for userId: {}", userId);

        List<ItemUserPermission> blocks = permissionService.getReadableBlocks(userId);
        List<ItemPermissionResponse> response = ItemPermissionResponse.fromEntities(blocks);

        return ResponseEntity.ok(response);
    }

    // ==================== ПРОВЕРКА ДОСТУПА ====================

    /**
     * POST /api/v1/permissions/check
     * Проверить наличие прав доступа к элементу
     */
    @PostMapping("/check")
    @Operation(summary = "Проверить права доступа",
            description = "Проверяет наличие запрошенных прав доступа к конкретному элементу")
    public ResponseEntity<PermissionCheckResponse> checkPermission(
            @CurrentUser @Parameter(hidden = true) UUID userId,
            @RequestBody CheckItemPermissionRequest request) {

        log.info("Checking permission for userId: {}, itemId: {}, itemType: {}, requiredPermissions: {}",
                userId, request.getItemId(), request.getItemType(), request.getRequiredPermissions());

        // Проверяем права доступа
        boolean hasPermission = permissionService.hasPermission(
                userId,
                request.getItemId(),
                request.getItemType(),
                request.getRequiredPermissions()
        );

        // Получаем текущие права пользователя
        ItemUserPermission currentPermission = permissionService.getPermission(
                userId,
                request.getItemId(),
                request.getItemType()
        );

        // Формируем ответ
        PermissionCheckResponse response = PermissionCheckResponse.builder()
                .userId(userId)
                .itemId(request.getItemId())
                .itemType(request.getItemType().name())
                .requiredPermissions(request.getRequiredPermissions())
                .hasPermission(hasPermission)
                .actualPermissions(currentPermission != null ? currentPermission.getPermissions() : 0)
                .message(hasPermission ? "Access granted" : "Access denied")
                .build();

        log.info("Permission check result: {} for userId: {}, itemId: {}",
                hasPermission, userId, request.getItemId());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/permissions/check/{itemType}/{itemId}
     * Быстрая проверка прав READ
     */
    @GetMapping("/check/{itemType}/{itemId}")
    @Operation(summary = "Проверить права на чтение",
            description = "Быстрая проверка наличия прав READ к элементу")
    public ResponseEntity<PermissionCheckResponse> checkReadPermission(
            @CurrentUser @Parameter(hidden = true) UUID userId,
            @PathVariable ItemType itemType,
            @PathVariable UUID itemId) {

        log.debug("Checking READ permission for userId: {}, itemId: {}, itemType: {}",
                userId, itemId, itemType);

        boolean hasPermission = permissionService.hasReadPermission(userId, itemId, itemType);

        PermissionCheckResponse response = PermissionCheckResponse.builder()
                .userId(userId)
                .itemId(itemId)
                .itemType(itemType.name())
                .requiredPermissions(1) // READ
                .hasPermission(hasPermission)
                .message(hasPermission ? "Read access granted" : "No read access")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/permissions/item/{itemType}/{itemId}
     * Получить права доступа к конкретному элементу
     */
    @GetMapping("/item/{itemType}/{itemId}")
    @Operation(summary = "Получить права к элементу",
            description = "Возвращает текущие права доступа пользователя к элементу")
    public ResponseEntity<ItemPermissionResponse> getItemPermission(
            @CurrentUser @Parameter(hidden = true) UUID userId,
            @PathVariable ItemType itemType,
            @PathVariable UUID itemId) {

        log.debug("Getting permission for userId: {}, itemId: {}, itemType: {}",
                userId, itemId, itemType);

        ItemUserPermission permission = permissionService.getPermission(userId, itemId, itemType);

        if (permission == null) {
            log.warn("No permission found for userId: {}, itemId: {}", userId, itemId);
            return ResponseEntity.notFound().build();
        }

        ItemPermissionResponse response = ItemPermissionResponse.fromEntity(permission);

        return ResponseEntity.ok(response);
    }

    // ==================== СТАТИСТИКА ====================

    /**
     * GET /api/v1/permissions/statistics
     * Получить статистику доступа пользователя
     */
    @GetMapping("/statistics")
    @Operation(summary = "Получить статистику доступа",
            description = "Возвращает количество доступных элементов каждого типа")
    public ResponseEntity<ItemPermissionService.UserAccessStatistics> getStatistics(
            @CurrentUser @Parameter(hidden = true) UUID userId) {

        log.debug("Getting access statistics for userId: {}", userId);

        ItemPermissionService.UserAccessStatistics statistics =
                permissionService.getUserAccessStatistics(userId);

        return ResponseEntity.ok(statistics);
    }
}