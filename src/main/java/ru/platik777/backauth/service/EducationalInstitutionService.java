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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с образовательными учреждениями
 * Реализует логику из Go версии EducationalInstitutionsService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EducationalInstitutionService {

    private final EducationalInstitutionRepository educationalInstitutionRepository;

    /**
     * Поиск образовательных учреждений по названию (аналог EducationalInstitutionsFind из Go)
     */
    public ApiResponseDto<List<EducationalInstitutionDto>> findEducationalInstitutions(String name) {
        log.debug("Searching educational institutions by name: {}", name);

        // TODO: Временная блокировка как в Go версии
        log.warn("Educational institutions search is temporarily blocked");
        throw new RuntimeException("Educational institutions search is temporarily blocked");

        // Когда блокировка будет снята, раскомментировать:
        /*
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name parameter is required");
        }

        List<EducationalInstitution> institutions = educationalInstitutionRepository
                .findByFullNameContainingIgnoreCase(name.trim());

        List<EducationalInstitutionDto> institutionDtos = institutions.stream()
                .map(EducationalInstitutionDto::new)
                .collect(Collectors.toList());

        log.info("Found {} educational institutions for search: {}", institutionDtos.size(), name);

        return ApiResponseDto.success(institutionDtos);
        */
    }

    /**
     * Поиск образовательных учреждений с пагинацией
     */
    public ApiResponseDto<Page<EducationalInstitutionDto>> findEducationalInstitutionsWithPagination(
            String name, int page, int size) {

        log.debug("Searching educational institutions by name: {} with pagination", name);

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
     */
    public EducationalInstitutionDto getEducationalInstitution(String uuid) {
        log.debug("Getting educational institution by UUID: {}", uuid);

        EducationalInstitution institution = educationalInstitutionRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Educational institution not found"));

        return new EducationalInstitutionDto(institution);
    }

    /**
     * Получение активных образовательных учреждений
     */
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
     * Поиск образовательных учреждений по региону
     */
    public ApiResponseDto<List<EducationalInstitutionDto>> findByRegion(String regionName) {
        log.debug("Searching educational institutions by region: {}", regionName);

        if (regionName == null || regionName.trim().isEmpty()) {
            throw new RuntimeException("Region name parameter is required");
        }

        List<EducationalInstitution> institutions = educationalInstitutionRepository
                .findByRegionNameIgnoreCase(regionName.trim());

        List<EducationalInstitutionDto> institutionDtos = institutions.stream()
                .map(EducationalInstitutionDto::new)
                .collect(Collectors.toList());

        log.info("Found {} educational institutions in region: {}", institutionDtos.size(), regionName);

        return ApiResponseDto.success(institutionDtos);
    }

    /**
     * Поиск образовательных учреждений по типу
     */
    public ApiResponseDto<List<EducationalInstitutionDto>> findByType(String typeName) {
        log.debug("Searching educational institutions by type: {}", typeName);

        if (typeName == null || typeName.trim().isEmpty()) {
            throw new RuntimeException("Type name parameter is required");
        }

        List<EducationalInstitution> institutions = educationalInstitutionRepository
                .findByTypeNameIgnoreCase(typeName.trim());

        List<EducationalInstitutionDto> institutionDtos = institutions.stream()
                .map(EducationalInstitutionDto::new)
                .collect(Collectors.toList());

        log.info("Found {} educational institutions of type: {}", institutionDtos.size(), typeName);

        return ApiResponseDto.success(institutionDtos);
    }

    /**
     * Поиск образовательного учреждения по ИНН
     */
    public EducationalInstitutionDto findByInn(String inn) {
        log.debug("Searching educational institution by INN: {}", inn);

        EducationalInstitution institution = educationalInstitutionRepository.findByInn(inn)
                .orElseThrow(() -> new RuntimeException("Educational institution not found by INN"));

        return new EducationalInstitutionDto(institution);
    }

    /**
     * Поиск образовательного учреждения по ОГРН
     */
    public EducationalInstitutionDto findByOgrn(String ogrn) {
        log.debug("Searching educational institution by OGRN: {}", ogrn);

        EducationalInstitution institution = educationalInstitutionRepository.findByOgrn(ogrn)
                .orElseThrow(() -> new RuntimeException("Educational institution not found by OGRN"));

        return new EducationalInstitutionDto(institution);
    }

    /**
     * Проверка существования образовательного учреждения по UUID
     */
    public boolean existsByUuid(String uuid) {
        return educationalInstitutionRepository.existsByUuid(uuid);
    }

    /**
     * Получение статистики образовательных учреждений
     */
    public ApiResponseDto<?> getEducationalInstitutionStats() {
        log.debug("Getting educational institution statistics");

        long totalCount = educationalInstitutionRepository.count();
        long activeCount = educationalInstitutionRepository.findByIsActiveTrue().size();

        var stats = new EducationalInstitutionStats(totalCount, activeCount);

        return ApiResponseDto.success(stats);
    }

    // Внутренний класс для статистики
    public record EducationalInstitutionStats(long total, long active) {}
}