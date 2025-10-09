package ru.platik777.backauth.entity.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountType {
    INDIVIDUAL("individual"),
    STUDENT("student"),
    ENTREPRENEUR("entrepreneur"),
    COMPANY_RESIDENT("company_resident"),
    COMPANY_NON_RESIDENT("company_non_resident"),
    EDUCATIONAL_UNIT("educational_unit");

    private final String value;
}
