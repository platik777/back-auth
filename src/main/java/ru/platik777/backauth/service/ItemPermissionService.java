package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platik777.backauth.entity.*;
import ru.platik777.backauth.entity.types.ItemType;
import ru.platik777.backauth.entity.types.Permission;
import ru.platik777.backauth.exception.PermissionDeniedException;
import ru.platik777.backauth.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис управления правами доступа пользователей к элементам системы
 *
 * Работает с битовыми масками прав:
 * - READ (1): чтение
 * - WRITE (2): запись (требует READ)
 * - EXECUTE (4): выполнение (требует READ)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemPermissionService {

    private final ItemUserPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final BlockRepository blockRepository;


    /**
     * Выдать права доступа пользователю на элемент
     *
     * @param granterId ID пользователя, выдающего права (должен иметь WRITE)
     * @param targetUserId ID пользователя, которому выдаются права
     * @param itemId ID элемента
     * @param itemType тип элемента
     * @param permissions битовая маска прав (0-7)
     * @return созданная запись прав доступа
     * @throws PermissionDeniedException если нет прав на выдачу
     * @throws IllegalArgumentException если данные невалидны
     */
    @Transactional
    public ItemUserPermission grantPermission(UUID granterId, UUID targetUserId,
                                              UUID itemId, ItemType itemType,
                                              short permissions) {
        log.info("Granting permission: granter={}, target={}, item={}, type={}, permissions={}",
                granterId, targetUserId, itemId, itemType, permissions);

        validateUserId(granterId);
        validateUserId(targetUserId);
        validateItemId(itemId);
        validateItemType(itemType);
        Permission.validate(permissions);

        // Проверка что granter имеет право выдавать доступ (нужен WRITE)
        if (!hasReadWritePermission(granterId, itemId, itemType)) {
            throw new PermissionDeniedException(
                    "User " + granterId + " does not have WRITE permission to grant access to item " + itemId
            );
        }

        // Проверка что пользователь существует
        if (!userRepository.existsById(targetUserId)) {
            throw new IllegalArgumentException("Target user not found: " + targetUserId);
        }

        // Проверка что элемент существует
        validateItemExists(itemId, itemType);

        // Проверка что у пользователя еще нет прав на этот элемент
        Optional<ItemUserPermission> existing = getPermissionEntity(targetUserId, itemId, itemType);
        if (existing.isPresent()) {
            throw new IllegalStateException("User " + targetUserId + " already has permission for item " + itemId +
                            ". Use updatePermission() to modify existing permissions.");
        }

        // Создаем новую запись прав
        ItemUserPermission permission = createPermissionEntity(
                targetUserId, itemId, itemType, permissions
        );

        ItemUserPermission saved = permissionRepository.save(permission);

        log.info("Permission granted successfully: id={}, user={}, item={}, permissions={}",
                saved.getId(), targetUserId, itemId, permissions);

        return saved;
    }

    /**
     * Обновить права доступа пользователя на элемент
     *
     * @param updaterId ID пользователя, обновляющего права (должен иметь WRITE)
     * @param targetUserId ID пользователя, чьи права обновляются
     * @param itemId ID элемента
     * @param itemType тип элемента
     * @param newPermissions новая битовая маска прав
     * @return обновленная запись прав доступа
     */
    @Transactional
    public ItemUserPermission updatePermission(UUID updaterId, UUID targetUserId,
                                               UUID itemId, ItemType itemType,
                                               short newPermissions) {
        log.info("Updating permission: updater={}, target={}, item={}, newPermissions={}",
                updaterId, targetUserId, itemId, newPermissions);

        validateUserId(updaterId);
        validateUserId(targetUserId);
        validateItemId(itemId);
        validateItemType(itemType);
        Permission.validate(newPermissions);

        // Проверка прав updater'а
        if (!hasReadWritePermission(updaterId, itemId, itemType)) {
            throw new PermissionDeniedException(
                    "User " + updaterId + " does not have WRITE permission to update access"
            );
        }

        // Получаем существующую запись
        ItemUserPermission permission = getPermissionEntity(targetUserId, itemId, itemType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Permission not found for user " + targetUserId + " and item " + itemId
                ));

        short oldPermissions = permission.getPermissions();

        // Обновляем права
        permission.setPermissions(newPermissions);
        ItemUserPermission updated = permissionRepository.save(permission);

        log.info("Permission updated successfully: id={}, user={}, item={}, oldPermissions={}, newPermissions={}",
                updated.getId(), targetUserId, itemId, oldPermissions, newPermissions);

        return updated;
    }

    /**
     * Отозвать права доступа у пользователя
     *
     * @param revokerId ID пользователя, отзывающего права (должен иметь WRITE)
     * @param targetUserId ID пользователя, у которого отзываются права
     * @param itemId ID элемента
     * @param itemType тип элемента
     */
    @Transactional
    public void revokePermission(UUID revokerId, UUID targetUserId,
                                 UUID itemId, ItemType itemType) {
        log.info("Revoking permission: revoker={}, target={}, item={}, type={}",
                revokerId, targetUserId, itemId, itemType);

        // Валидация
        validateUserId(revokerId);
        validateUserId(targetUserId);
        validateItemId(itemId);
        validateItemType(itemType);

        // Проверка прав revoker'а
        if (!hasReadWritePermission(revokerId, itemId, itemType)) {
            throw new PermissionDeniedException(
                    "User " + revokerId + " does not have WRITE permission to revoke access"
            );
        }

        // Находим и удаляем запись
        ItemUserPermission permission = getPermissionEntity(targetUserId, itemId, itemType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Permission not found for user " + targetUserId + " and item " + itemId
                ));

        permissionRepository.delete(permission);

        log.info("Permission revoked successfully: user={}, item={}", targetUserId, itemId);
    }

    /**
     * Получить все проекты, доступные пользователю на чтение
     */
    @Transactional(readOnly = true)
    public List<ItemUserPermission> getReadableProjects(UUID userId) {
        log.debug("Getting readable projects for userId: {}", userId);

        validateUserId(userId);

        List<ItemUserPermission> projects = permissionRepository.findReadableProjectsByUserId(userId);

        log.debug("Found {} readable projects for userId: {}", projects.size(), userId);
        return projects;
    }

    /**
     * Получить все папки, доступные пользователю на чтение
     */
    @Transactional(readOnly = true)
    public List<ItemUserPermission> getReadableFolders(UUID userId) {
        log.debug("Getting readable folders for userId: {}", userId);

        validateUserId(userId);

        List<ItemUserPermission> folders = permissionRepository.findReadableFoldersByUserId(userId);

        log.debug("Found {} readable folders for userId: {}", folders.size(), userId);
        return folders;
    }

    /**
     * Получить все файлы, доступные пользователю на чтение
     */
    @Transactional(readOnly = true)
    public List<ItemUserPermission> getReadableFiles(UUID userId) {
        log.debug("Getting readable files for userId: {}", userId);

        validateUserId(userId);

        List<ItemUserPermission> files = permissionRepository.findReadableFilesByUserId(userId);

        log.debug("Found {} readable files for userId: {}", files.size(), userId);
        return files;
    }

    /**
     * Получить все блоки, доступные пользователю на чтение
     */
    @Transactional(readOnly = true)
    public List<ItemUserPermission> getReadableBlocks(UUID userId) {
        log.debug("Getting readable blocks for userId: {}", userId);

        validateUserId(userId);

        List<ItemUserPermission> blocks = permissionRepository.findReadableBlocksByUserId(userId);

        log.debug("Found {} readable blocks for userId: {}", blocks.size(), userId);
        return blocks;
    }

    /**
     * Получить все элементы всех типов, доступные пользователю на чтение
     */
    @Transactional(readOnly = true)
    public List<ItemUserPermission> getAllReadableItems(UUID userId) {
        log.debug("Getting all readable items for userId: {}", userId);

        validateUserId(userId);

        List<ItemUserPermission> items = permissionRepository.findAllReadableItemsByUserId(userId);

        log.debug("Found {} readable items for userId: {}", items.size(), userId);
        return items;
    }

    // ==================== ПРОВЕРКА ДОСТУПА ====================

    /**
     * Проверить наличие конкретных прав доступа к элементу
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, UUID itemId, ItemType itemType, int requiredPermissions) {
        log.debug("Checking permission for userId: {}, itemId: {}, itemType: {}, requiredPermissions: {}",
                userId, itemId, itemType, requiredPermissions);

        validateUserId(userId);
        validateItemId(itemId);
        validateItemType(itemType);
        validatePermissionValue(requiredPermissions);

        if (requiredPermissions != 0 && (requiredPermissions & 1) == 0) {
            throw new IllegalArgumentException(
                    "Invalid required permissions: " + requiredPermissions +
                            ". Cannot check WRITE or EXECUTE without READ"
            );
        }

        boolean hasPermission = switch (itemType) {
            case PROJECT -> permissionRepository.hasProjectPermissions(
                    userId, itemId, (short) requiredPermissions
            );
            case FOLDER -> permissionRepository.hasFolderPermissions(
                    userId, itemId, (short) requiredPermissions
            );
            case FILE -> permissionRepository.hasFilePermissions(
                    userId, itemId, (short) requiredPermissions
            );
            case BLOCK -> permissionRepository.hasBlockPermissions(
                    userId, itemId, (short) requiredPermissions
            );
        };

        log.debug("Permission check result: {} for userId: {}, itemId: {}, itemType: {}",
                hasPermission, userId, itemId, itemType);

        return hasPermission;
    }

    /**
     * Проверить наличие прав READ
     */
    @Transactional(readOnly = true)
    public boolean hasReadPermission(UUID userId, UUID itemId, ItemType itemType) {
        return hasPermission(userId, itemId, itemType, Permission.READ.getValue());
    }

    /**
     * Проверить наличие прав READ + WRITE
     */
    @Transactional(readOnly = true)
    public boolean hasReadWritePermission(UUID userId, UUID itemId, ItemType itemType) {
        int permissions = Permission.combine(Permission.READ, Permission.WRITE);
        return hasPermission(userId, itemId, itemType, permissions);
    }

    /**
     * Проверить наличие прав READ + EXECUTE
     */
    @Transactional(readOnly = true)
    public boolean hasReadExecutePermission(UUID userId, UUID itemId, ItemType itemType) {
        int permissions = Permission.combine(Permission.READ, Permission.EXECUTE);
        return hasPermission(userId, itemId, itemType, permissions);
    }

    /**
     * Проверить наличие всех прав (READ + WRITE + EXECUTE)
     */
    @Transactional(readOnly = true)
    public boolean hasFullPermission(UUID userId, UUID itemId, ItemType itemType) {
        int permissions = Permission.combine(Permission.READ, Permission.WRITE, Permission.EXECUTE);
        return hasPermission(userId, itemId, itemType, permissions);
    }

    /**
     * Получить права доступа пользователя к конкретному элементу
     */
    @Transactional(readOnly = true)
    public ItemUserPermission getPermission(UUID userId, UUID itemId, ItemType itemType) {
        log.debug("Getting permission for userId: {}, itemId: {}, itemType: {}",
                userId, itemId, itemType);

        validateUserId(userId);
        validateItemId(itemId);
        validateItemType(itemType);

        return getPermissionEntity(userId, itemId, itemType).orElse(null);
    }

    // ==================== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Получить сущность прав доступа
     */
    private Optional<ItemUserPermission> getPermissionEntity(UUID userId, UUID itemId, ItemType itemType) {
        return switch (itemType) {
            case PROJECT -> permissionRepository.findByUserIdAndProjectId(userId, itemId);
            case FOLDER -> permissionRepository.findByUserIdAndFolderId(userId, itemId);
            case FILE -> permissionRepository.findByUserIdAndFileId(userId, itemId);
            case BLOCK -> permissionRepository.findByUserIdAndBlockId(userId, itemId);
        };
    }

    /**
     * Создать новую сущность прав доступа
     */
    private ItemUserPermission createPermissionEntity(UUID userId, UUID itemId,
                                                      ItemType itemType, short permissions) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ItemUserPermission permission = ItemUserPermission.builder()
                .user(user)
                .permissions(permissions)
                .build();

        // Привязываем к соответствующему элементу
        switch (itemType) {
            case PROJECT -> {
                Project project = projectRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Project not found: " + itemId));
                permission.setProject(project);
            }
            case FOLDER -> {
                Folder folder = folderRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + itemId));
                permission.setFolder(folder);
            }
            case FILE -> {
                ru.platik777.backauth.entity.File file = fileRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("File not found: " + itemId));
                permission.setFile(file);
            }
            case BLOCK -> {
                Block block = blockRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Block not found: " + itemId));
                permission.setBlock(block);
            }
        }

        return permission;
    }

    /**
     * Проверить что элемент существует
     */
    private void validateItemExists(UUID itemId, ItemType itemType) {
        boolean exists = switch (itemType) {
            case PROJECT -> projectRepository.existsById(itemId);
            case FOLDER -> folderRepository.existsById(itemId);
            case FILE -> fileRepository.existsById(itemId);
            case BLOCK -> blockRepository.existsById(itemId);
        };

        if (!exists) {
            throw new IllegalArgumentException(
                    itemType + " not found: " + itemId
            );
        }
    }

    // ==================== ВАЛИДАЦИЯ ====================

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    private void validateItemId(UUID itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("ItemId cannot be null");
        }
    }

    private void validateItemType(ItemType itemType) {
        if (itemType == null) {
            throw new IllegalArgumentException("ItemType cannot be null");
        }
    }

    private void validatePermissionValue(int permissions) {
        if (permissions < 0 || permissions > 7) {
            throw new IllegalArgumentException(
                    "Permissions must be between 0 and 7, got: " + permissions
            );
        }
    }

    // ==================== СТАТИСТИКА ====================

    /**
     * Получить статистику доступа пользователя
     */
    @Transactional(readOnly = true)
    public UserAccessStatistics getUserAccessStatistics(UUID userId) {
        log.debug("Getting access statistics for userId: {}", userId);

        validateUserId(userId);

        long projectCount = permissionRepository.findReadableProjectsByUserId(userId).size();
        long folderCount = permissionRepository.findReadableFoldersByUserId(userId).size();
        long fileCount = permissionRepository.findReadableFilesByUserId(userId).size();
        long blockCount = permissionRepository.findReadableBlocksByUserId(userId).size();

        return new UserAccessStatistics(
                userId,
                projectCount,
                folderCount,
                fileCount,
                blockCount,
                projectCount + folderCount + fileCount + blockCount
        );
    }

    // ==================== INNER CLASSES ====================

    /**
     * DTO для статистики доступа пользователя
     */
    public record UserAccessStatistics(
            UUID userId,
            long readableProjects,
            long readableFolders,
            long readableFiles,
            long readableBlocks,
            long totalReadableItems
    ) {}

}