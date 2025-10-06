package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.platik777.backauth.entity.keys.ItemUserPermissionId;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "item_user_permission")
@IdClass(ItemUserPermissionId.class)
@Getter
@Setter
public class ItemUserPermission implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    ItemUserPermissionId id;

    @Column(name = "user_id")
    UUID userId;

    @Column(name = "tenant_id")
    UUID tenantId;

    @Column(name = "permissions")
    Integer permissions; // 0-7
}
