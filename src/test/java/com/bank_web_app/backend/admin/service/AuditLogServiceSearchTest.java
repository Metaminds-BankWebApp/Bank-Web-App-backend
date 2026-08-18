package com.bank_web_app.backend.admin.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditLogServiceSearchTest {

    @Test
    void normalizeSearchPattern_shouldBeCaseInsensitive() {
        String search = AuditLogService.normalizeSearchPattern("ADMIN");

        assertTrue(search != null && search.equals("%admin%"));
    }

    @Test
    void matchesSearchIgnoreCase_shouldMatchMixedCaseValues() {
        boolean result = AuditLogService.matchesSearchIgnoreCase("Kamal Edirisinghe", "kamal");

        assertTrue(result);
    }

    @Test
    void matchesSearchIgnoreCase_shouldMatchStatusAndDateText() {
        assertTrue(AuditLogService.matchesSearchIgnoreCase("SUCCESS", "success"));
        assertTrue(AuditLogService.matchesSearchIgnoreCase("2026-08-18T09:46:03", "2026-08-18"));
    }

    @Test
    void parseDateSearchQuery_shouldAcceptTheDateFormatDisplayedInTheAuditTable() {
        assertEquals(LocalDate.of(2026, 8, 18), AuditLogService.parseDateSearchQuery("18 Aug 2026"));
        assertEquals(LocalDate.of(2026, 8, 18), AuditLogService.parseDateSearchQuery("18/08/2026"));
    }

    @Test
    void tonesForDisplayStatusSearch_shouldMapTheVisibleStatusLabelsToStoredTones() {
        assertEquals(List.of("SUCCESS"), AuditLogService.tonesForDisplayStatusSearch("Success"));
        assertEquals(List.of("ERROR"), AuditLogService.tonesForDisplayStatusSearch("Failure"));
        assertEquals(List.of("WARNING", "INFO"), AuditLogService.tonesForDisplayStatusSearch("Policy Change"));
    }
}
