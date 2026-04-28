package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;
import ticket_train.ticketeer.dto.mobile.TarificationResponse;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.enums.ProfilTarifaire;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class TarificationService {

    public TarificationResponse buildTarificationResponse(ServiceFerroviaire service,
                                                          Optional<ServiceFerroviaire> returnService,
                                                          String profilTarifaireValue,
                                                          String classeReservation) {
        ProfilTarifaire profilTarifaire = parseProfil(profilTarifaireValue);
        double prixBase = computeBasePrice(service, returnService, classeReservation);

        TarificationResponse response = new TarificationResponse();
        response.setServiceId(service.getServiceId().toString());
        response.setProfilTarifaire(profilTarifaire.name());
        response.setPrixBase(prixBase);
        response.setPrixFinal(applyDiscount(prixBase, profilTarifaire));
        return response;
    }

    public ProfilTarifaire parseProfil(String value) {
        if (value == null || value.isBlank()) {
            return ProfilTarifaire.STANDARD;
        }
        try {
            return ProfilTarifaire.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ProfilTarifaire.STANDARD;
        }
    }

    public double applyDiscount(double prixBase, ProfilTarifaire profilTarifaire) {
        double coefficient = switch (profilTarifaire) {
            case ENFANT_MOINS_7 -> 0.25;
            case ETUDIANT_DECLARE -> 0.80;
            case SENIOR_65_PLUS -> 0.70;
            case HANDICAP_DECLARE -> 0.60;
            case STANDARD -> 1.00;
        };
        return BigDecimal.valueOf(prixBase * coefficient).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public double computeBasePrice(ServiceFerroviaire service,
                                   Optional<ServiceFerroviaire> returnService,
                                   String classeReservation) {
        double prixBase = service.getPrixBase() + returnService.map(ServiceFerroviaire::getPrixBase).orElse(0.0);
        double classMultiplier = "PREMIERE".equalsIgnoreCase(normalizeClass(classeReservation)) ? 1.35 : 1.00;
        return BigDecimal.valueOf(prixBase * classMultiplier).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public String normalizeClass(String value) {
        if (value == null || value.isBlank()) {
            return "SECONDE";
        }
        String normalized = value.trim().toUpperCase();
        return normalized.startsWith("PREM") ? "PREMIERE" : "SECONDE";
    }
}
