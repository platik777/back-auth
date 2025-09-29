package ru.platik777.backauth.controller;

import ru.platik777.backauth.dto.EducationalInstitutionDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.service.EducationalInstitutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для работы с образовательными учреждениями
 * Реализует эндпоинты из handler.go
 *
 * Paths:
 * - /api/v1/educationalInstitutions/* - эндпоинты для поиска учреждений
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/educationalInstitutions")
@RequiredArgsConstructor
public class EducationalInstitutionController {

    private final EducationalInstitutionService educationalInstitutionService;

    /**
     * Поиск образовательных учреждений по названию
     * GET /api/v1/educationalInstitutions/find?name={name}
     * Аналог educationalInstitutionsFind из Go handler
     */
    @GetMapping("/find")
    public ResponseEntity<ApiResponseDto<List<EducationalInstitutionDto>>> findEducationalInstitutions(
            @RequestParam String name) {

        log.info("Find educational institutions request for name: {}", name);

        ApiResponseDto<List<EducationalInstitutionDto>> response =
                educationalInstitutionService.findEducationalInstitutions(name);

        return ResponseEntity.ok(response);
    }

    /**
     * Поиск образовательных учреждений с пагинацией
     * GET /api/v1/educationalInstitutions/search?name={name}&page={page}&size={size}
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<Page<EducationalInstitutionDto>>> searchEducationalInstitutions(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Search educational institutions request for name: {} (page: {}, size: {})", name, page, size);

        ApiResponseDto<Page<EducationalInstitutionDto>> response =
                educationalInstitutionService.findEducationalInstitutionsWithPagination(name, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * Получение образовательного учреждения по UUID
     * GET /api/v1/educationalInstitutions/{uuid}
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDto<EducationalInstitutionDto>> getEducationalInstitution(
            @PathVariable String uuid) {

        log.info("Get educational institution request for UUID: {}", uuid);

        ApiResponseDto<EducationalInstitutionDto> response =
                educationalInstitutionService.getEducationalInstitution(uuid);

        return ResponseEntity.ok(response);
    }

    /**
     * Получение активных образовательных учреждений
     * GET /api/v1/educationalInstitutions/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponseDto<List<EducationalInstitutionDto>>> getActiveEducationalInstitutions() {

        log.info("Get active educational institutions request");

        ApiResponseDto<List<EducationalInstitutionDto>> response =
                educationalInstitutionService.getActiveEducationalInstitutions();

        return ResponseEntity.ok(response);
    }

    /**
     * Получение образовательных учреждений по типу
     * GET /api/v1/educationalInstitutions/byType?typeName={typeName}
     */
    @GetMapping("/byType")
    public ResponseEntity<ApiResponseDto<List<EducationalInstitutionDto>>> getEducationalInstitutionsByType(
            @RequestParam String typeName) {

        log.info("Get educational institutions by type request for type: {}", typeName);

        ApiResponseDto<List<EducationalInstitutionDto>> response =
                educationalInstitutionService.getEducationalInstitutionsByType(typeName);

        return ResponseEntity.ok(response);
    }

    /**
     * Поиск образовательного учреждения по ИНН
     * GET /api/v1/educationalInstitutions/byInn?inn={inn}
     */
    @GetMapping("/byInn")
    public ResponseEntity<ApiResponseDto<EducationalInstitutionDto>> findByInn(
            @RequestParam String inn) {

        log.info("Find educational institution by INN request for INN: {}", inn);

        ApiResponseDto<EducationalInstitutionDto> response =
                educationalInstitutionService.findByInn(inn);

        return ResponseEntity.ok(response);
    }

    /**
     * Поиск образовательного учреждения по ОГРН
     * GET /api/v1/educationalInstitutions/byOgrn?ogrn={ogrn}
     */
    @GetMapping("/byOgrn")
    public ResponseEntity<ApiResponseDto<EducationalInstitutionDto>> findByOgrn(
            @RequestParam String ogrn) {

        log.info("Find educational institution by OGRN request for OGRN: {}", ogrn);

        ApiResponseDto<EducationalInstitutionDto> response =
                educationalInstitutionService.findByOgrn(ogrn);

        return ResponseEntity.ok(response);
    }

    /**
     * Получение статистики образовательных учреждений
     * GET /api/v1/educationalInstitutions/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDto<EducationalInstitutionService.EducationalInstitutionStats>> getEducationalInstitutionStats() {

        log.info("Get educational institution statistics request");

        ApiResponseDto<EducationalInstitutionService.EducationalInstitutionStats> response =
                educationalInstitutionService.getEducationalInstitutionStats();

        return ResponseEntity.ok(response);
    }
}