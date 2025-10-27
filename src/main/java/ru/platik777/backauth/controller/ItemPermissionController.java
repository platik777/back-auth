package ru.platik777.backauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.backauth.dto.response.PermissionResponse;
import ru.platik777.backauth.entity.ItemUserPermission;
import ru.platik777.backauth.entity.types.ItemType;
import ru.platik777.backauth.service.ItemPermissionService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class ItemPermissionController {

    private final ItemPermissionService permissionService;

    // ==================== УПРАВЛЕНИЕ ПРАВАМИ ====================

    /**
     * Выдать права доступа
     */
    @PostMapping("/grant")
    public ResponseEntity<ItemUserPermission> grantPermission(
            @RequestHeader("X-User-Id") String granterId,
            @RequestParam String targetUserId,
            @RequestParam String itemId,
            @RequestParam ItemType itemType,
            @RequestParam short permissions) {

        log.info("Grant permission request: granter={}, target={}, item={}, type={}, permissions={}",
                granterId, targetUserId, itemId, itemType, permissions);

        ItemUserPermission result = permissionService.grantPermission(
                granterId, targetUserId, itemId, itemType, permissions
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Обновить права доступа
     */
    @PutMapping("/update")
    public ResponseEntity<ItemUserPermission> updatePermission(
            @RequestHeader("X-User-Id") String updaterId,
            @RequestParam String targetUserId,
            @RequestParam String itemId,
            @RequestParam ItemType itemType,
            @RequestParam short newPermissions) {

        log.info("Update permission request: updater={}, target={}, item={}, type={}, newPermissions={}",
                updaterId, targetUserId, itemId, itemType, newPermissions);

        ItemUserPermission result = permissionService.updatePermission(
                updaterId, targetUserId, itemId, itemType, newPermissions
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Отозвать права доступа
     */
    @DeleteMapping("/revoke")
    public ResponseEntity<Void> revokePermission(
            @RequestHeader("X-User-Id") String revokerId,
            @RequestParam String targetUserId,
            @RequestParam String itemId,
            @RequestParam ItemType itemType) {

        log.info("Revoke permission request: revoker={}, target={}, item={}, type={}",
                revokerId, targetUserId, itemId, itemType);

        permissionService.revokePermission(revokerId, targetUserId, itemId, itemType);

        return ResponseEntity.noContent().build();
    }

    // ==================== ПОЛУЧЕНИЕ ПРАВ ====================

    /**
     * Получить эффективные права на проект
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<PermissionResponse> getProjectPermissions(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String userId) {

        Short permissions = permissionService.getProjectPermissions(userId, projectId);

        return ResponseEntity.ok(new PermissionResponse(
                projectId,
                ItemType.PROJECT,
                permissions
        ));
    }

    /**
     * Получить эффективные права на блок
     */
    @GetMapping("/block/{blockId}")
    public ResponseEntity<PermissionResponse> getBlockPermissions(
            @PathVariable String blockId,
            @RequestHeader("X-User-Id") String userId) {

        Short permissions = permissionService.getBlockPermissions(userId, blockId);

        return ResponseEntity.ok(new PermissionResponse(
                blockId,
                ItemType.BLOCK,
                permissions
        ));
    }

    /**
     * Получить эффективные права на файл
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<PermissionResponse> getFilePermissions(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId) {

        Short permissions = permissionService.getFilePermissions(userId, fileId);

        return ResponseEntity.ok(new PermissionResponse(
                fileId,
                ItemType.FILE,
                permissions
        ));
    }

    /**
     * Получить эффективные права на папку
     */
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<PermissionResponse> getFolderPermissions(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId) {

        Short permissions = permissionService.getFolderPermissions(userId, folderId);

        return ResponseEntity.ok(new PermissionResponse(
                folderId,
                ItemType.FOLDER,
                permissions
        ));
    }

    // ==================== ПРОВЕРКИ ПРАВ ====================

    /**
     * Проверить возможность чтения проекта
     */
    @GetMapping("/project/{projectId}/can-read")
    public ResponseEntity<Boolean> canReadProject(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(permissionService.canReadProject(userId, projectId));
    }

    /**
     * Проверить возможность записи в проект
     */
    @GetMapping("/project/{projectId}/can-write")
    public ResponseEntity<Boolean> canWriteProject(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(permissionService.canWriteProject(userId, projectId));
    }

    /**
     * Проверить возможность выполнения проекта
     */
    @GetMapping("/project/{projectId}/can-execute")
    public ResponseEntity<Boolean> canExecuteProject(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(permissionService.canExecuteProject(userId, projectId));
    }

    // ==================== МАССОВЫЕ ОПЕРАЦИИ ====================

    /**
     * Получить права для нескольких проектов
     */
    @PostMapping("/projects/batch")
    public ResponseEntity<Map<String, Short>> getProjectPermissionsBatch(
            @RequestBody List<String> projectIds,
            @RequestHeader("X-User-Id") String userId) {

        Map<String, Short> permissions = permissionService.getProjectPermissionsBatch(userId, projectIds);

        return ResponseEntity.ok(permissions);
    }

    /**
     * Получить права для нескольких блоков
     */
    @PostMapping("/blocks/batch")
    public ResponseEntity<Map<String, Short>> getBlockPermissionsBatch(
            @RequestBody List<String> blockIds,
            @RequestHeader("X-User-Id") String userId) {

        Map<String, Short> permissions = permissionService.getBlockPermissionsBatch(userId, blockIds);

        return ResponseEntity.ok(permissions);
    }

    // ==================== ДОСТУПНЫЕ ЭЛЕМЕНТЫ ====================

    /**
     * Получить все доступные проекты
     */
    @GetMapping("/projects/accessible")
    public ResponseEntity<List<ItemPermissionService.ItemWithPermissions>> getAccessibleProjects(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<ItemPermissionService.ItemWithPermissions> projects =
                permissionService.getAllAccessibleProjects(userId, tenantId);

        return ResponseEntity.ok(projects);
    }

    /**
     * Получить все доступные блоки
     */
    @GetMapping("/blocks/accessible")
    public ResponseEntity<List<ItemPermissionService.ItemWithPermissions>> getAccessibleBlocks(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<ItemPermissionService.ItemWithPermissions> blocks =
                permissionService.getAllAccessibleBlocks(userId, tenantId);

        return ResponseEntity.ok(blocks);
    }

    /**
     * Получить все доступные файлы
     */
    @GetMapping("/files/accessible")
    public ResponseEntity<List<ItemPermissionService.ItemWithPermissions>> getAccessibleFiles(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<ItemPermissionService.ItemWithPermissions> files =
                permissionService.getAllAccessibleFiles(userId, tenantId);

        return ResponseEntity.ok(files);
    }

    /**
     * Получить все доступные папки
     */
    @GetMapping("/folders/accessible")
    public ResponseEntity<List<ItemPermissionService.FolderWithPermissions>> getAccessibleFolders(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<ItemPermissionService.FolderWithPermissions> folders =
                permissionService.getAllAccessibleFolders(userId, tenantId);

        return ResponseEntity.ok(folders);
    }

    // ==================== СОДЕРЖИМОЕ ПАПКИ ====================

    /**
     * Получить содержимое папки с правами доступа
     */
    @GetMapping("/folder/{folderId}/contents")
    public ResponseEntity<ItemPermissionService.FolderContents> getFolderContents(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId) {

        ItemPermissionService.FolderContents contents =
                permissionService.getFolderContents(userId, folderId);

        return ResponseEntity.ok(contents);
    }

    /**
     * Получить проекты в папке
     */
    @GetMapping("/folder/{folderId}/projects")
    public ResponseEntity<List<ItemPermissionService.ItemWithPermissions>> getProjectsInFolder(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId) {

        List<ItemPermissionService.ItemWithPermissions> projects =
                permissionService.getProjectsInFolder(userId, folderId);

        return ResponseEntity.ok(projects);
    }

    /**
     * Получить блоки в папке
     */
    @GetMapping("/folder/{folderId}/blocks")
    public ResponseEntity<List<ItemPermissionService.ItemWithPermissions>> getBlocksInFolder(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId) {

        List<ItemPermissionService.ItemWithPermissions> blocks =
                permissionService.getBlocksInFolder(userId, folderId);

        return ResponseEntity.ok(blocks);
    }

    /**
     * Получить файлы в папке
     */
    @GetMapping("/folder/{folderId}/files")
    public ResponseEntity<List<ItemPermissionService.ItemWithPermissions>> getFilesInFolder(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId) {

        List<ItemPermissionService.ItemWithPermissions> files =
                permissionService.getFilesInFolder(userId, folderId);

        return ResponseEntity.ok(files);
    }

    /**
     * Получить дочерние папки
     */
    @GetMapping("/folder/{folderId}/subfolders")
    public ResponseEntity<List<ItemPermissionService.FolderWithPermissions>> getSubfolders(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId) {

        List<ItemPermissionService.FolderWithPermissions> subfolders =
                permissionService.getSubfolders(userId, folderId);

        return ResponseEntity.ok(subfolders);
    }

    // ==================== СТАТИСТИКА ====================

    /**
     * Подсчитать доступные проекты
     */
    @GetMapping("/projects/count")
    public ResponseEntity<Long> countAccessibleProjects(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "1") short minPermissions) {

        long count = permissionService.countAccessibleProjects(userId, tenantId, minPermissions);

        return ResponseEntity.ok(count);
    }

    /**
     * Подсчитать доступные блоки
     */
    @GetMapping("/blocks/count")
    public ResponseEntity<Long> countAccessibleBlocks(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "1") short minPermissions) {

        long count = permissionService.countAccessibleBlocks(userId, tenantId, minPermissions);

        return ResponseEntity.ok(count);
    }
}