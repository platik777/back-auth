package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.EducationalInstitutionDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.entity.EducationalInstitution;
import ru.platik777.backauth.repository.EducationalInstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с образовательными учреждениями
 * Реализует логику EducationalInstitutionsFind из Go
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EducationalInstitutionService {

    private final EducationalInstitutionRepository educationalInstitutionRepository;

    /**
     * Поиск образовательных учреждений по названию
     * Аналог EducationalInstitutionsFind из Go
     *
     * @param name Название учреждения (частичное совпадение)
     * @return Список найденных учреждений
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<List<EducationalInstitutionDto>> findEducationalInstitutions(String name) {
        log.debug("Searching educational institutions by name: {}", name);

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name parameter is required");
        }

        // Поиск по полному названию (ILIKE в PostgreSQL = ignoring case)
        List<EducationalInstitution> institutions = educationalInstitutionRepository
                .findByFullNameContainingIgnoreCase(name.trim());

        List<EducationalInstitutionDto> institutionDtos = institutions.stream()
                .map(EducationalInstitutionDto::new)
                .collect(Collectors.toList());

        log.info("Found {} educational institutions for search: {}", institutionDtos.size(), name);

        return ApiResponseDto.success(institutionDtos);
    }

    /**
     * Поиск образовательных учреждений с пагинацией
     *
     * @param name Название учреждения
     * @param page Номер страницы (начиная с 0)
     * @param size Размер страницы
     * @return Страница с учреждениями
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<Page<EducationalInstitutionDto>> findEducationalInstitutionsWithPagination(
            String name, int page, int size) {

        log.debug("Searching educational institutions by name: {} with pagination (page: {}, size: {})",
                name, page, size);

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name parameter is required");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<EducationalInstitution> institutionPage = educationalInstitutionRepository
                .findByFullNameContainingIgnoreCase(name.trim(), pageable);

        Page<EducationalInstitutionDto> institutionDtoPage = institutionPage
                .map(EducationalInstitutionDto::new);

        log.info("Found {} educational institutions for search: {} (page {}, size {})",
                institutionDtoPage.getTotalElements(), name, page, size);

        return ApiResponseDto.success(institutionDtoPage);
    }

    /**
     * Получение образовательного учреждения по UUID
     *
     * @param uuid UUID учреждения
     * @return Данные учреждения
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<EducationalInstitutionDto> getEducationalInstitution(String uuid) {
        log.debug("Getting educational institution by UUID: {}", uuid);

        EducationalInstitution institution = educationalInstitutionRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Educational institution not found"));

        EducationalInstitutionDto dto = new EducationalInstitutionDto(institution);

        return ApiResponseDto.success(dto);
    }

    /**
     * Получение активных образовательных учреждений
     *
     * @return Список активных учреждений
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<List<EducationalInstitutionDto>> getActiveEducationalInstitutions() {
        log.debug("Getting active educational institutions");

        List<EducationalInstitution> institutions = educationalInstitutionRepository.findByIsActiveTrue();

        List<EducationalInstitutionDto> institutionDtos = institutions.stream()
                .map(EducationalInstitutionDto::new)
                .collect(Collectors.toList());

        log.info("Found {} active educational institutions", institutionDtos.size());

        return ApiResponseDto.success(institutionDtos);
    }

    /**
     * Получение образовательных учреждений по типу
     *
     * @param typeName Тип учреждения
     * @return Список учреждений указанного типа
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<List<EducationalInstitutionDto>> getEducationalInstitutionsByType(String typeName) {
        log.debug("Getting educational institutions by type: {}", typeName);

        List<EducationalInstitution> institutions = educationalInstitutionRepository
                .findByTypeName(typeName);

        List<EducationalInstitutionDto> institutionDtos = institutions.stream()
                .map(EducationalInstitutionDto::new)
                .collect(Collectors.toList());

        log.info("Found {} educational institutions of type: {}", institutionDtos.size(), typeName);

        return ApiResponseDto.success(institutionDtos);
    }

    /**
     * Поиск образовательного учреждения по ИНН
     *
     * @param inn ИНН учреждения
     * @return Данные учреждения
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<EducationalInstitutionDto> findByInn(String inn) {
        log.debug("Searching educational institution by INN: {}", inn);

        EducationalInstitution institution = educationalInstitutionRepository.findByInn(inn)
                .orElseThrow(() -> new RuntimeException("Educational institution not found by INN"));

        EducationalInstitutionDto dto = new EducationalInstitutionDto(institution);

        return ApiResponseDto.success(dto);
    }

    /**
     * Поиск образовательного учреждения по ОГРН
     *
     * @param ogrn ОГРН учреждения
     * @return Данные учреждения
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<EducationalInstitutionDto> findByOgrn(String ogrn) {
        log.debug("Searching educational institution by OGRN: {}", ogrn);

        EducationalInstitution institution = educationalInstitutionRepository.findByOgrn(ogrn)
                .orElseThrow(() -> new RuntimeException("Educational institution not found by OGRN"));

        EducationalInstitutionDto dto = new EducationalInstitutionDto(institution);

        return ApiResponseDto.success(dto);
    }

    /**
     * Проверка существования образовательного учреждения по UUID
     *
     * @param uuid UUID учреждения
     * @return true если существует
     */
    public boolean existsByUuid(String uuid) {
        return educationalInstitutionRepository.existsByUuid(uuid);
    }

    /**
     * Получение статистики образовательных учреждений
     *
     * @return Статистика (общее количество и количество активных)
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<EducationalInstitutionStats> getEducationalInstitutionStats() {
        log.debug("Getting educational institution statistics");

        long totalCount = educationalInstitutionRepository.count();
        long activeCount = educationalInstitutionRepository.findByIsActiveTrue().size();

        EducationalInstitutionStats stats = new EducationalInstitutionStats(totalCount, activeCount);

        log.info("Educational institution stats - Total: {}, Active: {}", totalCount, activeCount);

        return ApiResponseDto.success(stats);
    }

    /**
     * Record для статистики образовательных учреждений
     */
    public record EducationalInstitutionStats(long total, long active) {}
}