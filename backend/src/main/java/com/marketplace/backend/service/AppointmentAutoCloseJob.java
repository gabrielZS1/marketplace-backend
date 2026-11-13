package com.marketplace.backend.service;

import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Encerra sozinho os agendamentos cujo horário já passou — até agora ninguém
 * marcava "concluído", então reservas antigas ficavam como "próximas" para sempre
 * (e as estatísticas de atendimentos/faturamento nunca contavam nada).
 *
 *  - confirmada ou em andamento e o horário de término já passou  → CONCLUÍDA
 *  - pendente (o estabelecimento nunca confirmou) e já passou     → CANCELADA
 *    (a reserva não chegou a acontecer, e não pode ficar "aguardando" para sempre)
 *
 * É idempotente: rodar em mais de uma instância ao mesmo tempo não causa problema.
 */
@Component
public class AppointmentAutoCloseJob {

    private static final Logger log = LoggerFactory.getLogger(AppointmentAutoCloseJob.class);

    private final AppointmentRepository appointmentRepository;

    public AppointmentAutoCloseJob(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    // A cada 5 minutos (espera 1 min depois de subir, para não competir com a inicialização).
    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    @Transactional
    public void closeEndedAppointments() {
        OffsetDateTime now = OffsetDateTime.now();

        int completed = appointmentRepository.closeEnded(
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS),
                AppointmentStatus.COMPLETED,
                now
        );

        int expired = appointmentRepository.closeEnded(
                List.of(AppointmentStatus.PENDING),
                AppointmentStatus.CANCELLED,
                now
        );

        if (completed > 0 || expired > 0) {
            log.info("Agendamentos encerrados automaticamente: {} concluído(s), {} pendente(s) cancelado(s)",
                    completed, expired);
        }
    }
}
