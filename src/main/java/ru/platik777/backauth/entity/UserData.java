package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "user_data")
@Data
@EqualsAndHashCode(callSuper = false)
public class UserData {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "login", nullable = false, length = 255)
    private String login;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(name = "phone", length = 30)
    private String phone;
}