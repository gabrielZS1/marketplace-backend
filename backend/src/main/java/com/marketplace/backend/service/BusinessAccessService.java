package com.marketplace.backend.service;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Controla o acesso ao estabelecimento: dono tem acesso total, funcionário ativo
 * tem acesso de leitura à agenda/clientes. Gerência continua só do dono.
 */
@Service
public class BusinessAccessService {

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;

    public BusinessAccessService(BusinessRepository businessRepository,
                                 EmployeeRepository employeeRepository) {
        this.businessRepository = businessRepository;
        this.employeeRepository = employeeRepository;
    }

    /** O estabelecimento de quem está logado — dono OU funcionário ativo. */
    public Optional<Business> resolveBusiness(UUID userId) {
        Optional<Business> owned = businessRepository
                .findFirstByOwnerIdOrderByOnboardingCompletedDescCreatedAtAsc(userId);
        if (owned.isPresent()) return owned;
        return employeeRepository.findByIdAndActiveTrue(userId).map(Employee::getBusiness);
    }

    public boolean isOwner(Business business, UUID userId) {
        return business.getOwner().getId().equals(userId);
    }

    public boolean isEmployeeOf(Business business, UUID userId) {
        return employeeRepository.findByIdAndActiveTrue(userId)
                .map(e -> e.getBusiness().getId().equals(business.getId()))
                .orElse(false);
    }

    public boolean isMember(Business business, UUID userId) {
        return isOwner(business, userId) || isEmployeeOf(business, userId);
    }

    public boolean isEmployee(UUID userId) {
        return employeeRepository.findByIdAndActiveTrue(userId).isPresent();
    }

    /** Exige que o usuário seja dono. 403 (mensagem com "permissão") caso contrário. */
    public Business requireOwner(UUID businessId, UUID userId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        if (!isOwner(business, userId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }
        return business;
    }

    /** Exige que o usuário seja dono ou funcionário ativo do estabelecimento. */
    public Business requireMember(UUID businessId, UUID userId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        if (!isMember(business, userId)) {
            throw new RuntimeException("Você não tem permissão para ver esta agenda");
        }
        return business;
    }
}
