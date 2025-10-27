package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "folder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Folder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "all_parent_ids", columnDefinition = "text[]")
    private List<String> allParentIds;

    @Column
    private Integer rank;

    @Column(name = "has_children")
    private Boolean hasChildren = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "item_types", columnDefinition = "text[]")
    private List<String> itemTypes;
}