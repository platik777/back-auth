package ru.platik777.backauth.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings implements Serializable {
    private String locale = "ru";
    private String theme;
    private Boolean isEngineer = false;
    private Object iconType;
}