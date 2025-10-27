package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "item_group_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ItemGroupPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private Short permission;

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
     * Валидация перед сохранением - должен быть заполнен ровно один из project/folder/file/block
     */
    @PrePersist
    @PreUpdate
    public void validate() {
        int count = 0;
        if (project != null) count++;
        if (folder != null) count++;
        if (file != null) count++;
        if (block != null) count++;

        if (count != 1) {
            throw new IllegalStateException(
                    "Exactly one of project, folder, file, or block must be set"
            );
        }
    }
}