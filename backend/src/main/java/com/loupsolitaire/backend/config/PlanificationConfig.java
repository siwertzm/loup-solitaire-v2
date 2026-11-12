package com.loupsolitaire.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// OPS-05 : active les taches planifiees (@Scheduled, voir PurgeService).
// Une seule instance sur Render : pas de verrou entre instances (ShedLock).
@Configuration
@EnableScheduling
public class PlanificationConfig {
}
