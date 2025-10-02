package ru.platik777.backauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.backauth.dto.request.SupportMessageRequest;
import ru.platik777.backauth.dto.response.EducationalInstitutionSearchResponse;
import ru.platik777.backauth.dto.response.StatusResponse;
import ru.platik777.backauth.service.EducationalInstitutionService;
import ru.platik777.backauth.service.InternalService;

/**
 * Контроллер внутренних операций и образовательных учреждений
 * Соответствует internal.go endpoints
 *
 * Go routes:
 * - apiv1Internal := apiv1R.PathPrefix("/internal").Subrouter()
 * - apiv1REducationalInstitutionsR := apiv1R.PathPrefix("/educationalInstitutions").Subrouter()
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalController {

    private final InternalService internalService;
    private final EducationalInstitutionService educationalInstitutionService;

    /**
     * POST /api/v1/internal/sendMessageToSupport
     * Отправка сообщения в техподдержку
     * Go: apiv1Internal.HandleFunc("/sendMessageToSupport", h.sendMessageToSupport).Methods(http.MethodPost)
     */
    @PostMapping("/api/v1/internal/sendMessageToSupport")
    public ResponseEntity<StatusResponse> sendMessageToSupport(
            @RequestBody SupportMessageRequest request) {

        log.info("Support message from: {} ({})", request.getUserName(), request.getEmail());

        StatusResponse response = internalService.sendMessageToSupport(
                request.getUserName(),
                request.getEmail(),
                request.getMessage(),
                request.getTargetSubject()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/educationalInstitutions/find
     * Поиск образовательных учреждений
     * Go: apiv1REducationalInstitutionsR.HandleFunc("/find", h.educationalInstitutionsFind).Methods(http.MethodGet)
     */
    @GetMapping("/api/v1/educationalInstitutions/find")
    public ResponseEntity<EducationalInstitutionSearchResponse> findEducationalInstitutions(
            @RequestParam String name) {

        log.info("Searching educational institutions: {}", name);

        EducationalInstitutionSearchResponse response =
                educationalInstitutionService.findEducationalInstitutions(name);

        return ResponseEntity.ok(response);
    }
}