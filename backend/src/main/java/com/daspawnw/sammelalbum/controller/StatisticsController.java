package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.StatisticsDtos.StatisticsResponse;
import com.daspawnw.sammelalbum.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics() {
        StatisticsResponse statistics = statisticsService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
