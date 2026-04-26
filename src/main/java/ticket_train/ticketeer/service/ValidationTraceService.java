package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.ValidationRepository;

import java.util.List;

@Service
public class ValidationTraceService {

    private final ValidationRepository validationRepository;

    public ValidationTraceService(ValidationRepository validationRepository) {
        this.validationRepository = validationRepository;
    }

    public void saveTrace(Controleur controleur,
                          SegmentBillet segment,
                          ValidationResult resultat,
                          ValidationMotif motif) {
        validationRepository.save(new Validation(resultat, motif, controleur, segment));
    }

    public List<Validation> getRecentTracesForController(Controleur controleur) {
        return validationRepository.findTop10ByControleurOrderByTimestampControleDesc(controleur);
    }
}
