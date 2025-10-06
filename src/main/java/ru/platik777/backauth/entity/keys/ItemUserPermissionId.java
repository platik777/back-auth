package ru.platik777.backauth.entity.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.platik777.backauth.entity.types.ItemType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ItemUserPermissionId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "item_type")
    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemUserPermissionId that = (ItemUserPermissionId) o;
        return Objects.equals(itemId, that.itemId) && itemType == that.itemType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, itemType);
    }
}