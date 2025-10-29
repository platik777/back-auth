package ru.platik777.backauth.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Block extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "all_parent_ids", columnDefinition = "text[]")
    private List<String> allParentIds;

    @Column
    private Integer rank;

    @Column(length = 255)
    private String name;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column
    private Boolean state;

    @Column(length = 10)
    private String locale;

    @Column(name = "category_id", length = 255)
    private String categoryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = true)
    private Image image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @OneToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}