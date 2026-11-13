package com.marketplace.backend.service;

import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.UserBlockRepository;
import com.marketplace.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Exclusão de conta (LGPD). Anonimiza a linha do usuário em vez de apagá-la,
 * porque agendamentos e avaliações são também dados do estabelecimento / de
 * terceiros. O acesso é encerrado e nenhum dado pessoal identificável fica.
 */
@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenService refreshTokenService;
    private final SubscriptionStatusService subscriptionStatusService;
    private final AppointmentRepository appointmentRepository;
    private final UserBlockRepository userBlockRepository;

    public AccountDeletionService(
            UserRepository userRepository,
            BusinessRepository businessRepository,
            EmployeeRepository employeeRepository,
            RefreshTokenService refreshTokenService,
            SubscriptionStatusService subscriptionStatusService,
            AppointmentRepository appointmentRepository,
            UserBlockRepository userBlockRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userBlockRepository = userBlockRepository;
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.employeeRepository = employeeRepository;
        this.refreshTokenService = refreshTokenService;
        this.subscriptionStatusService = subscriptionStatusService;
    }

    @Transactional
    public void delete(User user) {
        List<Business> owned = businessRepository.findByOwnerId(user.getId());

        for (Business business : owned) {
            SubscriptionStatus status = subscriptionStatusService.effectiveStatus(business);
            if (status == SubscriptionStatus.ACTIVE) {
                throw new RuntimeException(
                        "Cancele sua assinatura antes de excluir a conta.");
            }
        }

        // Desativa o estabelecimento e a equipe (o dono está saindo).
        for (Business business : owned) {
            business.setActive(false);
            businessRepository.save(business);
            for (Employee employee : employeeRepository.findByBusinessIdAndActiveTrue(business.getId())) {
                employee.setActive(false);
                employeeRepository.save(employee);
            }
        }

        // Se a pessoa é funcionária de um estabelecimento, sai da equipe — senão ela continuaria
        // aparecendo (como "Conta excluída") e podendo receber reservas.
        employeeRepository.findByIdAndActiveTrue(user.getId()).ifPresent(employee -> {
            employee.setActive(false);
            employeeRepository.save(employee);
        });

        // Cancela reservas futuras do cliente — senão o estabelecimento ficaria esperando
        // alguém que não existe mais.
        OffsetDateTime now = OffsetDateTime.now();
        for (Appointment appointment : appointmentRepository.findByClientId(user.getId())) {
            boolean open = appointment.getStatus() == AppointmentStatus.PENDING
                    || appointment.getStatus() == AppointmentStatus.CONFIRMED;
            if (open && appointment.getStartsAt().isAfter(now)) {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(appointment);
            }
        }

        // Bloqueios em que a pessoa participa deixam de fazer sentido.
        userBlockRepository.deleteByBlockerIdOrBlockedId(user.getId(), user.getId());

        // Anonimiza o usuário.
        String tag = "deleted-" + user.getId();
        user.setName("Conta excluída");
        user.setEmail(tag + "@glowly.invalid");
        user.setPhone(null);
        user.setPasswordHash(null);
        user.setGoogleId(null);
        user.setPhotoUrl(null);
        user.setAddress(null);
        user.setExpoPushToken(null);
        user.setEmailVerified(false);
        user.setActive(false);
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user);
    }
}
