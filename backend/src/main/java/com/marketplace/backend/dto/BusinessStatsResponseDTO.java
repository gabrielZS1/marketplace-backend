package com.marketplace.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Relatório do estabelecimento em um período (padrão: últimos 30 dias).
 * Todos os valores em reais; datas em ISO-8601.
 */
public record BusinessStatsResponseDTO(
        int periodDays,
        OffsetDateTime from,
        OffsetDateTime to,

        Appointments appointments,
        Revenue revenue,
        Clients clients,
        List<ServiceStat> topServices,
        List<EmployeeStat> team,
        Reviews reviews,
        Busiest busiest
) {
    public record Appointments(
            long total,
            long completed,
            long confirmed,
            long pending,
            long inProgress,
            long cancelled,
            long noShow,
            double completionRate,   // completed / (completed + cancelled + noShow)
            double cancellationRate  // (cancelled + noShow) / total
    ) {}

    public record Revenue(
            BigDecimal realized,   // soma dos serviços concluídos
            BigDecimal upcoming,   // agendado e ainda por vir (pendente/confirmado)
            BigDecimal lost,       // cancelados + no-show
            BigDecimal averageTicket
    ) {}

    public record Clients(
            long unique,
            long newInPeriod,
            long returning
    ) {}

    public record ServiceStat(
            String name,
            long count,
            BigDecimal revenue
    ) {}

    public record EmployeeStat(
            UUID employeeId,
            String name,
            long appointments,
            long completed,
            BigDecimal revenue,
            Double averageRating
    ) {}

    public record Reviews(
            Double average,       // média histórica do estabelecimento
            long countInPeriod,
            long stars5,
            long stars4,
            long stars3,
            long stars2,
            long stars1
    ) {}

    public record Busiest(
            String weekday,   // pt-BR, ex: "sexta-feira"
            Integer hour      // 0-23, hora de maior movimento
    ) {}
}
