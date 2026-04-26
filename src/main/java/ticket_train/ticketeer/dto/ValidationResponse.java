package ticket_train.ticketeer.dto;

import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;

public class ValidationResponse {
    private ValidationResult resultat;
    private ValidationMotif motif;
    private String clientNom;
    private String clientPrenom;
    private String clientPhotoRef;
    private String serviceTrain;
    private String serviceTrajet;
    private String serviceDate;
    private String checkpointControle;
    private String zoneValidite;

    public ValidationResponse() {}

    public ValidationResponse(ValidationResult resultat, ValidationMotif motif) {
        this.resultat = resultat;
        this.motif = motif;
    }

    public ValidationResult getResultat() { return resultat; }
    public void setResultat(ValidationResult resultat) { this.resultat = resultat; }
    public ValidationMotif getMotif() { return motif; }
    public void setMotif(ValidationMotif motif) { this.motif = motif; }
    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }
    public String getClientPrenom() { return clientPrenom; }
    public void setClientPrenom(String clientPrenom) { this.clientPrenom = clientPrenom; }
    public String getClientPhotoRef() { return clientPhotoRef; }
    public void setClientPhotoRef(String clientPhotoRef) { this.clientPhotoRef = clientPhotoRef; }
    public String getServiceTrain() { return serviceTrain; }
    public void setServiceTrain(String serviceTrain) { this.serviceTrain = serviceTrain; }
    public String getServiceTrajet() { return serviceTrajet; }
    public void setServiceTrajet(String serviceTrajet) { this.serviceTrajet = serviceTrajet; }
    public String getServiceDate() { return serviceDate; }
    public void setServiceDate(String serviceDate) { this.serviceDate = serviceDate; }
    public String getCheckpointControle() { return checkpointControle; }
    public void setCheckpointControle(String checkpointControle) { this.checkpointControle = checkpointControle; }
    public String getZoneValidite() { return zoneValidite; }
    public void setZoneValidite(String zoneValidite) { this.zoneValidite = zoneValidite; }
}
