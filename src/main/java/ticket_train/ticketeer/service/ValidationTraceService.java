package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.ValidationRepository;

import java.util.UUID;

@Service
public class ValidationTraceService {
    private final ValidationRepository validationRepository;

    public ValidationTraceService(ValidationRepository validationRepository) {
        this.validationRepository = validationRepository;
    }

    public Validation saveTrace(
            ValidationResult resultat,
            ValidationMotif motif,
            String codeOptiqueSaisi,
            UUID serviceIdDemande,
            Controleur controleur,
            SegmentBillet segment
    ) {
        Validation validation = new Validation(
                resultat,
                motif,
                codeOptiqueSaisi,
                serviceIdDemande,
                controleur,
                segment
        );
        return validationRepository.save(validation);
    }
}
