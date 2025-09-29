package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UniquenessResponseDto {

    private String unique;

    public static UniquenessResponseDto unique() {
        return new UniquenessResponseDto("true");
    }

    public static UniquenessResponseDto notUnique() {
        return new UniquenessResponseDto("false");
    }
}