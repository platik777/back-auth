package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Folder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "all_parant_ids", columnDefinition = "uuid[]")
    private List<UUID> allParantIds;

    @Column
    private Integer rank;

    @Column(name = "has_children")
    private Boolean hasChildren = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "item_types", columnDefinition = "text[]")
    private List<String> itemTypes;
}