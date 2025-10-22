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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис управления правами доступа пользователей к элементам системы
 * <p/>
 * Работает с битовыми масками прав:
 * - READ (1): чтение
 * - WRITE (2): запись (требует READ)
 * - EXECUTE (4): выполнение (требует READ)
 * <p/>
 * Поддерживает наследование прав от родительских папок
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

    // ==================== УПРАВЛЕНИЕ ПРАВАМИ (GRANT/UPDATE/REVOKE) ====================

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

    // ==================== ПОЛУЧЕНИЕ ЭФФЕКТИВНЫХ ПРАВ С НАСЛЕДОВАНИЕМ ====================

    /**
     * Получить эффективные права доступа к проекту (с учетом наследования от папок)
     */
    @Transactional(readOnly = true)
    public Short getProjectPermissions(UUID userId, UUID projectId) {
        log.debug("Getting project permissions for user={}, project={}", userId, projectId);

        // Теперь возвращается Short напрямую, а не ItemUserPermission
        Short permissions = permissionRepository.findEffectivePermissionForProject(userId, projectId);

        short result = permissions != null ? permissions : 0;

        log.debug("Project permissions result: {} for user={}, project={}", result, userId, projectId);
        return result;
    }

    /**
     * Получить эффективные права доступа к блоку
     */
    @Transactional(readOnly = true)
    public Short getBlockPermissions(UUID userId, UUID blockId) {
        log.debug("Getting block permissions for user={}, block={}", userId, blockId);

        Short permissions = permissionRepository.findEffectivePermissionForBlock(userId, blockId);

        short result = permissions != null ? permissions : 0;

        log.debug("Block permissions result: {} for user={}, block={}", result, userId, blockId);
        return result;
    }

    /**
     * Получить эффективные права доступа к файлу
     */
    @Transactional(readOnly = true)
    public Short getFilePermissions(UUID userId, UUID fileId) {
        log.debug("Getting file permissions for user={}, file={}", userId, fileId);

        Short permissions = permissionRepository.findEffectivePermissionForFile(userId, fileId);

        short result = permissions != null ? permissions : 0;

        log.debug("File permissions result: {} for user={}, file={}", result, userId, fileId);
        return result;
    }

    /**
     * Получить эффективные права доступа к папке
     */
    @Transactional(readOnly = true)
    public Short getFolderPermissions(UUID userId, UUID folderId) {
        log.debug("Getting folder permissions for user={}, folder={}", userId, folderId);

        Short permissions = permissionRepository.findEffectivePermissionForFolder(userId, folderId);

        short result = permissions != null ? permissions : 0;

        log.debug("Folder permissions result: {} for user={}, folder={}", result, userId, folderId);
        return result;
    }

    // ==================== ПРОВЕРКА ПРАВ ====================

    /**
     * Проверить наличие конкретных прав доступа к элементу
     * Использует эффективные права с учетом наследования
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
                    "Invalid required permissions: " + requiredPermissions + ". Cannot check WRITE or EXECUTE without READ"
            );
        }

        boolean hasPermission = switch (itemType) {
            case FOLDER -> permissionRepository.hasFolderPermission(userId, itemId, (short) requiredPermissions);
            case PROJECT -> permissionRepository.hasProjectPermission(userId, itemId, (short) requiredPermissions);
            case BLOCK -> permissionRepository.hasBlockPermission(userId, itemId, (short) requiredPermissions);
            case FILE -> permissionRepository.hasFilePermission(userId, itemId, (short) requiredPermissions);
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

    // ==================== БЫСТРЫЕ ПРОВЕРКИ ПО ТИПАМ ====================

    /**
     * Быстрая проверка прав на чтение проекта
     */
    @Transactional(readOnly = true)
    public boolean canReadProject(UUID userId, UUID projectId) {
        return permissionRepository.hasProjectPermission(userId, projectId, (short) 1);
    }

    /**
     * Быстрая проверка прав на запись в проект
     */
    @Transactional(readOnly = true)
    public boolean canWriteProject(UUID userId, UUID projectId) {
        return permissionRepository.hasProjectPermission(userId, projectId, (short) 3); // READ + WRITE
    }

    /**
     * Быстрая проверка прав на выполнение проекта
     */
    @Transactional(readOnly = true)
    public boolean canExecuteProject(UUID userId, UUID projectId) {
        return permissionRepository.hasProjectPermission(userId, projectId, (short) 5); // READ + EXECUTE
    }

    /**
     * Быстрая проверка прав на чтение блока
     */
    @Transactional(readOnly = true)
    public boolean canReadBlock(UUID userId, UUID blockId) {
        return permissionRepository.hasBlockPermission(userId, blockId, (short) 1);
    }

    /**
     * Быстрая проверка прав на запись в блок
     */
    @Transactional(readOnly = true)
    public boolean canWriteBlock(UUID userId, UUID blockId) {
        return permissionRepository.hasBlockPermission(userId, blockId, (short) 3);
    }

    /**
     * Быстрая проверка прав на файл
     */
    @Transactional(readOnly = true)
    public boolean canReadFile(UUID userId, UUID fileId) {
        return permissionRepository.hasFilePermission(userId, fileId, (short) 1);
    }

    /**
     * Быстрая проверка прав на запись в файл
     */
    @Transactional(readOnly = true)
    public boolean canWriteFile(UUID userId, UUID fileId) {
        return permissionRepository.hasFilePermission(userId, fileId, (short) 3);
    }

    /**
     * Быстрая проверка прав на папку
     */
    @Transactional(readOnly = true)
    public boolean canReadFolder(UUID userId, UUID folderId) {
        return permissionRepository.hasFolderPermission(userId, folderId, (short) 1);
    }

    /**
     * Быстрая проверка прав на запись в папку
     */
    @Transactional(readOnly = true)
    public boolean canWriteFolder(UUID userId, UUID folderId) {
        return permissionRepository.hasFolderPermission(userId, folderId, (short) 3);
    }

    // ==================== МАССОВЫЕ ОПЕРАЦИИ ====================

    /**
     * Получить права для нескольких проектов одновременно
     */
    @Transactional(readOnly = true)
    public Map<UUID, Short> getProjectPermissionsBatch(UUID userId, List<UUID> projectIds) {
        log.debug("Getting batch project permissions for user={}, count={}", userId, projectIds.size());

        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyMap();
        }

        UUID[] idsArray = projectIds.toArray(new UUID[0]);
        List<Object[]> results = permissionRepository.findEffectivePermissionsForProjects(userId, idsArray);

        Map<UUID, Short> permissions = results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).shortValue()
                ));

        log.debug("Batch project permissions result: {} items for user={}", permissions.size(), userId);
        return permissions;
    }

    /**
     * Получить права для нескольких блоков одновременно
     */
    @Transactional(readOnly = true)
    public Map<UUID, Short> getBlockPermissionsBatch(UUID userId, List<UUID> blockIds) {
        log.debug("Getting batch block permissions for user={}, count={}", userId, blockIds.size());

        if (blockIds == null || blockIds.isEmpty()) {
            return Collections.emptyMap();
        }

        UUID[] idsArray = blockIds.toArray(new UUID[0]);
        List<Object[]> results = permissionRepository.findEffectivePermissionsForBlocks(userId, idsArray);

        Map<UUID, Short> permissions = results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).shortValue()
                ));

        log.debug("Batch block permissions result: {} items for user={}", permissions.size(), userId);
        return permissions;
    }

    /**
     * Получить права для нескольких файлов одновременно
     */
    @Transactional(readOnly = true)
    public Map<UUID, Short> getFilePermissionsBatch(UUID userId, List<UUID> fileIds) {
        log.debug("Getting batch file permissions for user={}, count={}", userId, fileIds.size());

        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyMap();
        }

        UUID[] idsArray = fileIds.toArray(new UUID[0]);
        List<Object[]> results = permissionRepository.findEffectivePermissionsForFiles(userId, idsArray);

        Map<UUID, Short> permissions = results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).shortValue()
                ));

        log.debug("Batch file permissions result: {} items for user={}", permissions.size(), userId);
        return permissions;
    }

    /**
     * Получить права для нескольких папок одновременно
     */
    @Transactional(readOnly = true)
    public Map<UUID, Short> getFolderPermissionsBatch(UUID userId, List<UUID> folderIds) {
        log.debug("Getting batch folder permissions for user={}, count={}", userId, folderIds.size());

        if (folderIds == null || folderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        UUID[] idsArray = folderIds.toArray(new UUID[0]);
        List<Object[]> results = permissionRepository.findEffectivePermissionsForFolders(userId, idsArray);

        Map<UUID, Short> permissions = results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).shortValue()
                ));

        log.debug("Batch folder permissions result: {} items for user={}", permissions.size(), userId);
        return permissions;
    }

    // ==================== ПОЛУЧЕНИЕ ДОСТУПНЫХ ЭЛЕМЕНТОВ ====================

    /**
     * Получить все доступные проекты с их правами
     */
    @Transactional(readOnly = true)
    public List<ItemWithPermissions> getAllAccessibleProjects(UUID userId, String tenantId) {
        log.debug("Getting all accessible projects for user={}, tenant={}", userId, tenantId);

        List<Object[]> results = permissionRepository.findAllAccessibleProjects(userId, tenantId);

        List<ItemWithPermissions> projects = results.stream()
                .map(row -> new ItemWithPermissions(
                        (UUID) row[0],           // id
                        (UUID) row[1],           // folder_id
                        ((Number) row[6]).shortValue(), // effective_permissions
                        ((Timestamp) row[3]).toLocalDateTime(), // created_at
                        ItemType.PROJECT
                ))
                .collect(Collectors.toList());

        log.debug("Found {} accessible projects for user={}", projects.size(), userId);
        return projects;
    }

    /**
     * Получить все доступные блоки с их правами
     */
    @Transactional(readOnly = true)
    public List<ItemWithPermissions> getAllAccessibleBlocks(UUID userId, String tenantId) {
        log.debug("Getting all accessible blocks for user={}, tenant={}", userId, tenantId);

        List<Object[]> results = permissionRepository.findAllAccessibleBlocks(userId, tenantId);

        List<ItemWithPermissions> blocks = results.stream()
                .map(row -> new ItemWithPermissions(
                        (UUID) row[0],
                        (UUID) row[1],
                        ((Number) row[6]).shortValue(),
                        ((Timestamp) row[3]).toLocalDateTime(),
                        ItemType.BLOCK
                ))
                .collect(Collectors.toList());

        log.debug("Found {} accessible blocks for user={}", blocks.size(), userId);
        return blocks;
    }

    /**
     * Получить все доступные файлы с их правами
     */
    @Transactional(readOnly = true)
    public List<ItemWithPermissions> getAllAccessibleFiles(UUID userId, String tenantId) {
        log.debug("Getting all accessible files for user={}, tenant={}", userId, tenantId);

        List<Object[]> results = permissionRepository.findAllAccessibleFiles(userId, tenantId);

        List<ItemWithPermissions> files = results.stream()
                .map(row -> new ItemWithPermissions(
                        (UUID) row[0],
                        (UUID) row[1],
                        ((Number) row[6]).shortValue(),
                        ((Timestamp) row[3]).toLocalDateTime(),
                        ItemType.FILE
                ))
                .collect(Collectors.toList());

        log.debug("Found {} accessible files for user={}", files.size(), userId);
        return files;
    }

    /**
     * Получить все доступные папки с их правами
     */
    @Transactional(readOnly = true)
    public List<FolderWithPermissions> getAllAccessibleFolders(UUID userId, String tenantId) {
        log.debug("Getting all accessible folders for user={}, tenant={}", userId, tenantId);

        List<Object[]> results = permissionRepository.findAllAccessibleFolders(userId, tenantId);

        List<FolderWithPermissions> folders = results.stream()
                .map(row -> new FolderWithPermissions(
                        (UUID) row[0],           // id
                        (String) row[1],         // name
                        (UUID) row[2],           // parent_id
                        (Boolean) row[4],        // has_children
                        ((Number) row[8]).shortValue(), // effective_permissions
                        ((Timestamp) row[5]).toLocalDateTime() // created_at
                ))
                .collect(Collectors.toList());

        log.debug("Found {} accessible folders for user={}", folders.size(), userId);
        return folders;
    }

    // ==================== ПОЛУЧЕНИЕ ЭЛЕМЕНТОВ В ПАПКЕ ====================

    /**
     * Получить все проекты в папке с правами доступа
     */
    @Transactional(readOnly = true)
    public List<ItemWithPermissions> getProjectsInFolder(UUID userId, UUID folderId) {
        log.debug("Getting projects in folder={} for user={}", folderId, userId);

        List<Object[]> results = permissionRepository.findProjectsInFolderWithPermissions(userId, folderId);

        List<ItemWithPermissions> projects = results.stream()
                .map(row -> new ItemWithPermissions(
                        (UUID) row[0],
                        (UUID) row[1],
                        ((Number) row[4]).shortValue(),
                        ((Timestamp) row[3]).toLocalDateTime(),
                        ItemType.PROJECT
                ))
                .collect(Collectors.toList());

        log.debug("Found {} projects in folder={} for user={}", projects.size(), folderId, userId);
        return projects;
    }

    /**
     * Получить все блоки в папке с правами доступа
     */
    @Transactional(readOnly = true)
    public List<ItemWithPermissions> getBlocksInFolder(UUID userId, UUID folderId) {
        log.debug("Getting blocks in folder={} for user={}", folderId, userId);

        List<Object[]> results = permissionRepository.findBlocksInFolderWithPermissions(userId, folderId);

        List<ItemWithPermissions> blocks = results.stream()
                .map(row -> new ItemWithPermissions(
                        (UUID) row[0],
                        (UUID) row[1],
                        ((Number) row[4]).shortValue(),
                        ((Timestamp) row[3]).toLocalDateTime(),
                        ItemType.BLOCK
                ))
                .collect(Collectors.toList());

        log.debug("Found {} blocks in folder={} for user={}", blocks.size(), folderId, userId);
        return blocks;
    }

    /**
     * Получить все файлы в папке с правами доступа
     */
    @Transactional(readOnly = true)
    public List<ItemWithPermissions> getFilesInFolder(UUID userId, UUID folderId) {
        log.debug("Getting files in folder={} for user={}", folderId, userId);

        List<Object[]> results = permissionRepository.findFilesInFolderWithPermissions(userId, folderId);

        List<ItemWithPermissions> files = results.stream()
                .map(row -> new ItemWithPermissions(
                        (UUID) row[0],
                        (UUID) row[1],
                        ((Number) row[4]).shortValue(),
                        ((Timestamp) row[3]).toLocalDateTime(),
                        ItemType.FILE
                ))
                .collect(Collectors.toList());

        log.debug("Found {} files in folder={} for user={}", files.size(), folderId, userId);
        return files;
    }

    /**
     * Получить дочерние папки с правами доступа
     */
    @Transactional(readOnly = true)
    public List<FolderWithPermissions> getSubfolders(UUID userId, UUID parentFolderId) {
        log.debug("Getting subfolders of folder={} for user={}", parentFolderId, userId);

        List<Object[]> results = permissionRepository.findSubfoldersWithPermissions(userId, parentFolderId);

        List<FolderWithPermissions> subfolders = results.stream()
                .map(row -> new FolderWithPermissions(
                        (UUID) row[0],
                        (String) row[1],
                        (UUID) row[2],
                        (Boolean) row[4],
                        ((Number) row[6]).shortValue(),
                        ((Timestamp) row[5]).toLocalDateTime()
                ))
                .collect(Collectors.toList());

        log.debug("Found {} subfolders in folder={} for user={}", subfolders.size(), parentFolderId, userId);
        return subfolders;
    }

    /**
     * Получить все содержимое папки с правами доступа
     */
    @Transactional(readOnly = true)
    public FolderContents getFolderContents(UUID userId, UUID folderId) {
        log.debug("Getting folder contents for folder={}, user={}", folderId, userId);

        List<FolderWithPermissions> subfolders = getSubfolders(userId, folderId);
        List<ItemWithPermissions> projects = getProjectsInFolder(userId, folderId);
        List<ItemWithPermissions> blocks = getBlocksInFolder(userId, folderId);
        List<ItemWithPermissions> files = getFilesInFolder(userId, folderId);

        FolderContents contents = new FolderContents(subfolders, projects, blocks, files);

        log.debug("Folder contents for folder={}: {} subfolders, {} projects, {} blocks, {} files",
                folderId, subfolders.size(), projects.size(), blocks.size(), files.size());

        return contents;
    }

    // ==================== ПОДСЧЕТ ЭЛЕМЕНТОВ ====================

    /**
     * Подсчитать количество доступных проектов с минимальным уровнем прав
     */
    @Transactional(readOnly = true)
    public long countAccessibleProjects(UUID userId, String tenantId, short minPermissionMask) {
        log.debug("Counting accessible projects for user={}, tenant={}, minPermissions={}",
                userId, tenantId, minPermissionMask);

        long count = permissionRepository.countAccessibleProjects(userId, tenantId, minPermissionMask);

        log.debug("Found {} accessible projects for user={}", count, userId);
        return count;
    }

    /**
     * Подсчитать количество доступных блоков с минимальным уровнем прав
     */
    @Transactional(readOnly = true)
    public long countAccessibleBlocks(UUID userId, String tenantId, short minPermissionMask) {
        log.debug("Counting accessible blocks for user={}, tenant={}, minPermissions={}",
                userId, tenantId, minPermissionMask);

        long count = permissionRepository.countAccessibleBlocks(userId, tenantId, minPermissionMask);

        log.debug("Found {} accessible blocks for user={}", count, userId);
        return count;
    }

    // ==================== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Получить сущность прав доступа (только прямой доступ, без наследования)
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
    private ItemUserPermission createPermissionEntity(UUID userId, UUID itemId, ItemType itemType, short permissions) {
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
                File file = fileRepository.findById(itemId)
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

    // ==================== DTO КЛАССЫ ====================

    /**
     * DTO для элемента с правами доступа
     */
    public static class ItemWithPermissions {
        private final UUID id;
        private final UUID folderId;
        private final Short effectivePermissions;
        private final LocalDateTime createdAt;
        private final ItemType itemType;

        public ItemWithPermissions(UUID id, UUID folderId, Short effectivePermissions,
                                   LocalDateTime createdAt, ItemType itemType) {
            this.id = id;
            this.folderId = folderId;
            this.effectivePermissions = effectivePermissions;
            this.createdAt = createdAt;
            this.itemType = itemType;
        }

        public UUID getId() {
            return id;
        }

        public UUID getFolderId() {
            return folderId;
        }

        public Short getEffectivePermissions() {
            return effectivePermissions;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public ItemType getItemType() {
            return itemType;
        }

        public boolean canRead() {
            return (effectivePermissions & 1) != 0;
        }

        public boolean canWrite() {
            return (effectivePermissions & 2) != 0;
        }

        public boolean canExecute() {
            return (effectivePermissions & 4) != 0;
        }

        public String getPermissionsString() {
            StringBuilder sb = new StringBuilder();
            if (canRead()) sb.append("R");
            if (canWrite()) sb.append("W");
            if (canExecute()) sb.append("X");
            return sb.length() > 0 ? sb.toString() : "NONE";
        }

        @Override
        public String toString() {
            return "ItemWithPermissions{" +
                    "id=" + id +
                    ", folderId=" + folderId +
                    ", permissions=" + getPermissionsString() +
                    ", itemType=" + itemType +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }

    /**
     * DTO для папки с правами доступа
     */
    public static class FolderWithPermissions {
        private final UUID id;
        private final String name;
        private final UUID parentId;
        private final Boolean hasChildren;
        private final Short effectivePermissions;
        private final LocalDateTime createdAt;

        public FolderWithPermissions(UUID id, String name, UUID parentId, Boolean hasChildren,
                                     Short effectivePermissions, LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.hasChildren = hasChildren;
            this.effectivePermissions = effectivePermissions;
            this.createdAt = createdAt;
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public UUID getParentId() {
            return parentId;
        }

        public Boolean getHasChildren() {
            return hasChildren;
        }

        public Short getEffectivePermissions() {
            return effectivePermissions;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public boolean canRead() {
            return (effectivePermissions & 1) != 0;
        }

        public boolean canWrite() {
            return (effectivePermissions & 2) != 0;
        }

        public boolean canExecute() {
            return (effectivePermissions & 4) != 0;
        }

        public String getPermissionsString() {
            StringBuilder sb = new StringBuilder();
            if (canRead()) sb.append("R");
            if (canWrite()) sb.append("W");
            if (canExecute()) sb.append("X");
            return !sb.isEmpty() ? sb.toString() : "NONE";
        }

        @Override
        public String toString() {
            return "FolderWithPermissions{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", parentId=" + parentId +
                    ", hasChildren=" + hasChildren +
                    ", permissions=" + getPermissionsString() +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }

    /**
     * DTO для содержимого папки
     */
    public static class FolderContents {
        private final List<FolderWithPermissions> folders;
        private final List<ItemWithPermissions> projects;
        private final List<ItemWithPermissions> blocks;
        private final List<ItemWithPermissions> files;

        public FolderContents(List<FolderWithPermissions> folders,
                              List<ItemWithPermissions> projects,
                              List<ItemWithPermissions> blocks,
                              List<ItemWithPermissions> files) {
            this.folders = folders != null ? folders : Collections.emptyList();
            this.projects = projects != null ? projects : Collections.emptyList();
            this.blocks = blocks != null ? blocks : Collections.emptyList();
            this.files = files != null ? files : Collections.emptyList();
        }

        public List<FolderWithPermissions> getFolders() {
            return folders;
        }

        public List<ItemWithPermissions> getProjects() {
            return projects;
        }

        public List<ItemWithPermissions> getBlocks() {
            return blocks;
        }

        public List<ItemWithPermissions> getFiles() {
            return files;
        }

        public int getTotalCount() {
            return folders.size() + projects.size() + blocks.size() + files.size();
        }

        public boolean isEmpty() {
            return getTotalCount() == 0;
        }

        @Override
        public String toString() {
            return "FolderContents{" +
                    "folders=" + folders.size() +
                    ", projects=" + projects.size() +
                    ", blocks=" + blocks.size() +
                    ", files=" + files.size() +
                    ", total=" + getTotalCount() +
                    '}';
        }
    }
}