package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.NoTransactionException;
import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.*;
import ticket_train.ticketeer.model.enums.*;
import ticket_train.ticketeer.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Optional;
import java.util.UUID;

@Service
public class ValidationService {

    private final BilletRepository billetRepository;
    private final SegmentBilletRepository segmentBilletRepository;
    private final ServiceCheckpointRepository serviceCheckpointRepository;
    private final SignedQrService signedQrService;
    private final FraudDetectionService fraudDetectionService;
    private final ValidationTraceService validationTraceService;

    @Autowired
    public ValidationService(BilletRepository billetRepository,
                             SegmentBilletRepository segmentBilletRepository,
                             ServiceCheckpointRepository serviceCheckpointRepository,
                             ValidationRepository validationRepository,
                             ServiceFerroviaireRepository serviceFerroviaireRepository,
                             SignedQrService signedQrService,
                             FraudDetectionService fraudDetectionService,
                             ValidationTraceService validationTraceService) {
        this.billetRepository = billetRepository;
        this.segmentBilletRepository = segmentBilletRepository;
        this.serviceCheckpointRepository = serviceCheckpointRepository;
        this.signedQrService = signedQrService;
        this.fraudDetectionService = fraudDetectionService;
        this.validationTraceService = validationTraceService;
    }

    public ValidationService(BilletRepository billetRepository,
                             SegmentBilletRepository segmentBilletRepository,
                             ValidationRepository validationRepository,
                             ServiceFerroviaireRepository serviceFerroviaireRepository,
                             SignedQrService signedQrService,
                             FraudDetectionService fraudDetectionService,
                             ValidationTraceService validationTraceService) {
        this(
                billetRepository,
                segmentBilletRepository,
                null,
                validationRepository,
                serviceFerroviaireRepository,
                signedQrService,
                fraudDetectionService,
                validationTraceService
        );
    }

    @Transactional
    public ValidationResponse validerBillet(ValidationRequest request, Controleur controleur) {
        try {
            String codeOptique = request.getCodeOptique();
            UUID serviceId = request.getServiceId();
            UUID checkpointId = request.getCheckpointId();

            if (codeOptique == null || codeOptique.isBlank()) {
                return buildResponse(ValidationResult.INVALID, ValidationMotif.CODE_ILLISIBLE);
            }
            if (serviceId == null) {
                return buildResponse(ValidationResult.INVALID, ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT);
            }

            BilletLookupResult lookupResult = resolveBilletFromSubmittedCode(codeOptique);
            if (lookupResult.motif != null) {
                return buildResponse(ValidationResult.INVALID, lookupResult.motif);
            }

            Optional<Billet> billetOpt = lookupResult.billet;
            if (billetOpt.isEmpty()) {
                return buildResponse(ValidationResult.INVALID, ValidationMotif.BILLET_INCONNU);
            }

            Billet billet = billetOpt.get();
            Client client = billet.getClient();

            SegmentBillet segmentTrouve = null;
            for (SegmentBillet seg : billet.getSegments()) {
                if (seg.getService().getServiceId().equals(serviceId)) {
                    segmentTrouve = seg;
                    break;
                }
            }

            if (segmentTrouve == null) {
                ValidationResponse resp = buildResponse(ValidationResult.INVALID, ValidationMotif.NON_CONFORME_SERVICE);
                enrichWithClientInfo(resp, client, billet);
                return resp;
            }

            ServiceCheckpoint currentCheckpoint = resolveCurrentCheckpoint(segmentTrouve, checkpointId);
            if (currentCheckpoint == null || currentCheckpoint.getService() == null
                    || !currentCheckpoint.getService().getServiceId().equals(serviceId)) {
                ValidationResponse resp = buildResponse(ValidationResult.INVALID, ValidationMotif.NON_CONFORME_SERVICE);
                enrichWithClientInfo(resp, client, billet);
                return resp;
            }

            ValidationMotif fraudMotif = fraudDetectionService.detectValidationIssue(segmentTrouve);
            if (fraudMotif != null) {
                ValidationResponse resp = buildResponse(ValidationResult.INVALID, fraudMotif);
                enrichWithClientInfo(resp, client, billet);
                enrichWithJourneyContext(resp, segmentTrouve, currentCheckpoint);
                if (segmentTrouve != null) {
                    validationTraceService.saveTrace(controleur, segmentTrouve, ValidationResult.INVALID, fraudMotif);
                }
                return resp;
            }

            ValidationMotif journeyWindowIssue = detectJourneyWindowIssue(segmentTrouve, currentCheckpoint);
            if (journeyWindowIssue != null) {
                segmentTrouve.setEtatSegment(SegmentStatus.INVALIDE);
                segmentBilletRepository.save(segmentTrouve);
                ValidationResponse resp = buildResponse(ValidationResult.INVALID, journeyWindowIssue);
                enrichWithClientInfo(resp, client, billet);
                enrichWithJourneyContext(resp, segmentTrouve, currentCheckpoint);
                validationTraceService.saveTrace(controleur, segmentTrouve, ValidationResult.INVALID, journeyWindowIssue);
                return resp;
            }

            segmentTrouve.setEtatSegment(resolveNextSegmentStatus(segmentTrouve, currentCheckpoint));
            segmentBilletRepository.save(segmentTrouve);

            updateBilletStatus(billet);
            billetRepository.save(billet);

            validationTraceService.saveTrace(controleur, segmentTrouve, ValidationResult.VALID, ValidationMotif.OK);

            ValidationResponse resp = buildResponse(ValidationResult.VALID, ValidationMotif.OK);
            enrichWithClientInfo(resp, client, billet);
            enrichWithJourneyContext(resp, segmentTrouve, currentCheckpoint);

            ServiceFerroviaire sf = segmentTrouve.getService();
            resp.setServiceTrain(sf.getTrain().getNomTrain());
            resp.setServiceTrajet(sf.getVilleDepart().getNom() + " → " + sf.getVilleArrivee().getNom());
            resp.setServiceDate(sf.getDateTrajet().toString());

            return resp;
        } catch (DataAccessException | IllegalStateException ex) {
            markRollbackIfPossible();
            return buildResponse(ValidationResult.INVALID, ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT);
        }
    }

    private void enrichWithClientInfo(ValidationResponse resp, Client client, Billet billet) {
        resp.setClientNom(client.getNom());
        resp.setClientPrenom(client.getPrenom());
        resp.setClientPhotoRef(client.getPhotoRef());
    }

    private void enrichWithJourneyContext(ValidationResponse resp,
                                          SegmentBillet segmentBillet,
                                          ServiceCheckpoint currentCheckpoint) {
        resp.setCheckpointControle(currentCheckpoint.getOrdre() + ". " + currentCheckpoint.getVille().getNom());
        resp.setZoneValidite(segmentBillet.getService().getVilleDepart().getNom()
                + " -> "
                + segmentBillet.getService().getVilleArrivee().getNom()
                + " (checkpoints "
                + segmentBillet.getOrdreDepartValide()
                + "-"
                + segmentBillet.getOrdreArriveeValide()
                + ")");
    }

    private void updateBilletStatus(Billet billet) {
        boolean allProcessed = true;
        boolean anyProcessed = false;
        for (SegmentBillet seg : billet.getSegments()) {
            if (seg.getEtatSegment() == SegmentStatus.PREVU) {
                allProcessed = false;
            } else if (seg.getEtatSegment() == SegmentStatus.VALIDE || seg.getEtatSegment() == SegmentStatus.TERMINE) {
                anyProcessed = true;
                if (seg.getEtatSegment() != SegmentStatus.TERMINE) {
                    allProcessed = false;
                }
            } else {
                allProcessed = false;
            }
        }
        if (allProcessed) {
            billet.setEtat(TicketStatus.TERMINE);
        } else if (anyProcessed) {
            billet.setEtat(TicketStatus.EN_UTILISATION);
        }
    }

    private ValidationResponse buildResponse(ValidationResult resultat, ValidationMotif motif) {
        return new ValidationResponse(resultat, motif);
    }

    private void markRollbackIfPossible() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // Unit tests may invoke this service without a Spring transaction proxy.
        }
    }

    private BilletLookupResult resolveBilletFromSubmittedCode(String submittedCode) {
        SignedQrService.ParseResult parseResult = signedQrService.parseAndVerify(submittedCode);
        if (parseResult.getStatus() == SignedQrService.ParseStatus.NOT_SIGNED) {
            return new BilletLookupResult(billetRepository.findByCodeOptique(submittedCode), null);
        }
        if (parseResult.getStatus() == SignedQrService.ParseStatus.INVALID_SIGNATURE) {
            return new BilletLookupResult(Optional.empty(), ValidationMotif.QR_SIGNATURE_INVALIDE);
        }
        if (parseResult.getStatus() == SignedQrService.ParseStatus.MALFORMED) {
            return new BilletLookupResult(Optional.empty(), ValidationMotif.CODE_ILLISIBLE);
        }

        SignedQrService.SignedQrPayload qrPayload = parseResult.getPayload().orElseThrow();
        Optional<Billet> billetOpt = billetRepository.findById(qrPayload.getBilletId());
        if (billetOpt.isEmpty()) {
            return new BilletLookupResult(Optional.empty(), null);
        }

        Billet billet = billetOpt.get();
        boolean matches = qrPayload.getCodeOptique().equals(billet.getCodeOptique())
                && qrPayload.getClientId().equals(billet.getClient().getClientId());
        if (!matches) {
            return new BilletLookupResult(Optional.empty(), ValidationMotif.QR_SIGNATURE_INVALIDE);
        }

        return new BilletLookupResult(Optional.of(billet), null);
    }

    private ValidationMotif detectJourneyWindowIssue(SegmentBillet segmentBillet, ServiceCheckpoint checkpoint) {
        int currentOrder = checkpoint.getOrdre();
        int startOrder = segmentBillet.getOrdreDepartValide();
        int endOrder = segmentBillet.getOrdreArriveeValide();
        if (currentOrder < startOrder) {
            return ValidationMotif.AVANT_ZONE_DE_VALIDITE;
        }
        if (currentOrder > endOrder) {
            return ValidationMotif.HORS_PARCOURS_AUTORISE;
        }
        return null;
    }

    private SegmentStatus resolveNextSegmentStatus(SegmentBillet segmentBillet, ServiceCheckpoint checkpoint) {
        if (checkpoint.getOrdre() >= segmentBillet.getOrdreArriveeValide()) {
            return SegmentStatus.TERMINE;
        }
        return SegmentStatus.VALIDE;
    }

    private ServiceCheckpoint resolveCurrentCheckpoint(SegmentBillet segmentBillet, UUID checkpointId) {
        if (serviceCheckpointRepository != null && checkpointId != null) {
            return serviceCheckpointRepository.findById(checkpointId).orElse(null);
        }
        ServiceCheckpoint fallback = new ServiceCheckpoint();
        fallback.setService(segmentBillet.getService());
        fallback.setVille(segmentBillet.getService().getVilleDepart());
        fallback.setOrdre(1);
        return fallback;
    }

    private static final class BilletLookupResult {
        private final Optional<Billet> billet;
        private final ValidationMotif motif;

        private BilletLookupResult(Optional<Billet> billet, ValidationMotif motif) {
            this.billet = billet;
            this.motif = motif;
        }
    }
}
