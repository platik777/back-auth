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
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "all_parent_ids", columnDefinition = "text[]")
    private List<String> allParentIds;

    @Column
    private Integer rank;

    @Column(nullable = false, length = 255)
    private String name;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(length = 50)
    private String type;

    @Column(length = 50)
    private String status;

    @Column
    private Boolean favourite = false;

    @Column(length = 500)
    private String preview;
}