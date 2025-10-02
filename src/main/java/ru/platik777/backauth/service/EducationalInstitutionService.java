package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.platik777.backauth.dto.response.EducationalInstitutionResponse;
import ru.platik777.backauth.dto.response.EducationalInstitutionSearchResponse;
import ru.platik777.backauth.entity.EducationalInstitution;
import ru.platik777.backauth.repository.EducationalInstitutionRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис работы с образовательными учреждениями
 * Соответствует educationalInstitutions.go
 *
 * Go: type EducationalInstitutionsService struct
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EducationalInstitutionService {

    private final EducationalInstitutionRepository educationalInstitutionRepository;

    /**
     * Поиск образовательных учреждений по названию
     * Go: func (eI *EducationalInstitutionsService) EducationalInstitutionsFind(
     *         logger go_logger.Logger, name string) (response []byte, err error)
     *
     * Алгоритм:
     * 1. Валидация входных данных
     * 2. Нормализация поискового запроса
     * 3. Поиск в базе данных (ILIKE)
     * 4. Маппинг Entity → DTO
     * 5. Возврат результата
     *
     * @param name Название учреждения (часть названия)
     * @return EducationalInstitutionSearchResponse со списком найденных учреждений
     * @throws EducationalInstitutionException если валидация не прошла
     */
    public EducationalInstitutionSearchResponse findEducationalInstitutions(String name) {
        log.debug("Searching educational institutions with name: {}", name);

        // Валидация и нормализация входных данных
        String normalizedName = validateAndNormalizeName(name);

        try {
            // Поиск учреждений в БД
            // Go: list, err := eI.db.FindEducationalInstitution(logger, name)
            List<EducationalInstitution> institutions =
                    educationalInstitutionRepository.findByFullNameContainingIgnoreCase(normalizedName);

            // Логирование результатов
            if (institutions.isEmpty()) {
                log.info("No educational institutions found for: {}", normalizedName);
            } else {
                log.info("Found {} educational institutions for: {}",
                        institutions.size(), normalizedName);
            }

            // Маппинг Entity → DTO
            List<EducationalInstitutionResponse> responses = institutions.stream()
                    .map(EducationalInstitutionResponse::fromEntity)
                    .collect(Collectors.toList());

            // Формирование ответа
            // Go: respStruct := struct { Status bool; EducationalInstitutions []*models.EducationalInstitutions }
            return EducationalInstitutionSearchResponse.builder()
                    .status(true)
                    .educationalInstitutions(responses)
                    .build();

        } catch (Exception e) {
            log.error("Error searching educational institutions for: {}", normalizedName, e);
            throw new EducationalInstitutionException(
                    "Failed to search educational institutions",
                    e
            );
        }
    }

    /**
     * Получение образовательного учреждения по UUID
     * Go: GetEducationalInstitution (используется в GetStudent)
     *
     * @param uuid UUID учреждения
     * @return EducationalInstitution
     * @throws EducationalInstitutionException если не найдено
     */
    public EducationalInstitution getEducationalInstitution(String uuid) {
        log.debug("Getting educational institution by uuid: {}", uuid);

        if (!StringUtils.hasText(uuid)) {
            throw new EducationalInstitutionException("UUID cannot be empty");
        }

        return educationalInstitutionRepository.findById(uuid)
                .orElseThrow(() -> {
                    log.warn("Educational institution not found: {}", uuid);
                    return new EducationalInstitutionException(
                            "Educational institution not found: " + uuid
                    );
                });
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Валидация и нормализация имени для поиска
     *
     * @param name Поисковый запрос
     * @return Нормализованное имя
     * @throws EducationalInstitutionException если валидация не прошла
     */
    private String validateAndNormalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new EducationalInstitutionException("Search query cannot be empty");
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    // ==================== CUSTOM EXCEPTIONS ====================

    /**
     * Исключение при работе с образовательными учреждениями
     */
    public static class EducationalInstitutionException extends RuntimeException {
        public EducationalInstitutionException(String message) {
            super(message);
        }

        public EducationalInstitutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}