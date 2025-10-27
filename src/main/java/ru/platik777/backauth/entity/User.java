package ru.platik777.backauth.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import ru.platik777.backauth.entity.embedded.UserSettings;
import ru.platik777.backauth.entity.types.AccountType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String login;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private UserSettings settings;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 255)
    private AccountType accountType;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Tenant> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApiKey> apiKeys = new ArrayList<>();

    @ManyToMany(mappedBy = "users")
    @Builder.Default
    private Set<Group> groups = new HashSet<>();

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", login='" + login + '\'' +
                ", settings=" + settings +
                ", accountType=" + accountType +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}