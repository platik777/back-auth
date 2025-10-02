package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
// TODO: добавить общие поля
public class Tenant {
    @Id
    private Long id;

}