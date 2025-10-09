package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Check;

import java.util.UUID;

@Entity
@Table(
        name = "item_user_permission",
        indexes = {
                @Index(name = "idx_permission_user_project", columnList = "user_id, project_id"),
                @Index(name = "idx_permission_user_folder", columnList = "user_id, folder_id"),
                @Index(name = "idx_permission_user_file", columnList = "user_id, file_id"),
                @Index(name = "idx_permission_user_block", columnList = "user_id, block_id")
        }
)
@Check(constraints = """
    (project_id IS NOT NULL AND folder_id IS NULL AND file_id IS NULL AND block_id IS NULL) OR
    (project_id IS NULL AND folder_id IS NOT NULL AND file_id IS NULL AND block_id IS NULL) OR
    (project_id IS NULL AND folder_id IS NULL AND file_id IS NOT NULL AND block_id IS NULL) OR
    (project_id IS NULL AND folder_id IS NULL AND file_id IS NULL AND block_id IS NOT NULL)
    """)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ItemUserPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Short permissions; // Битовая маска 0-7

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    /**
     * Получить UUID элемента, к которому привязано разрешение
     */
    public UUID getItemId() {
        if (project != null) return project.getId();
        if (folder != null) return folder.getId();
        if (file != null) return file.getId();
        if (block != null) return block.getId();
        return null;
    }

    /**
     * Получить тип элемента
     */
    public String getItemType() {
        if (project != null) return "PROJECT";
        if (folder != null) return "FOLDER";
        if (file != null) return "FILE";
        if (block != null) return "BLOCK";
        return null;
    }
}