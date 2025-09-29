package ru.platik777.backauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.platik777.backauth.entity.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private Integer id;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    private String login;

    @JsonProperty("email")
    private String email;

    @JsonProperty("userName")
    private String userName;

    private String phone;

    @JsonProperty("accountType")
    private String accountType;

    private User.UserSettings settings;

    private List<String> roles;

    private List<String> permissions;

    @JsonProperty("isAdmin")
    private Boolean isAdmin = false;

    // Конструкторы
    public UserDto() {}

    public UserDto(User user) {
        this.id = user.getId();
        this.createdAt = user.getCreatedAt();
        this.login = user.getLogin();
        this.accountType = user.getAccountType().getValue();
        this.settings = user.getSettings();

        if (user.getUserData() != null) {
            this.email = user.getUserData().getEmail();
            this.userName = user.getUserData().getUserName();
            this.phone = user.getUserData().getPhone();
        }
    }
}