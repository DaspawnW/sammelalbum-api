package com.daspawnw.sammelalbum.scheduler;

import com.daspawnw.sammelalbum.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRequestScheduler {

    private final ExchangeService exchangeService;

    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    @SchedulerLock(name = "ExchangeRequestScheduler_processInitialRequests", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
    public void processInitialRequests() {
        log.info("Starting ExchangeRequestScheduler...");
        exchangeService.processInitialRequests();
        log.info("ExchangeRequestScheduler finished processing.");
    }
}
