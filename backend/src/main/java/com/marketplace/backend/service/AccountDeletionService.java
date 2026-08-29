package com.marketplace.backend.service;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
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

    public AccountDeletionService(
            UserRepository userRepository,
            BusinessRepository businessRepository,
            EmployeeRepository employeeRepository,
            RefreshTokenService refreshTokenService,
            SubscriptionStatusService subscriptionStatusService
    ) {
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
