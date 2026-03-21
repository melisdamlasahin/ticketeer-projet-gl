package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.SegmentBillet;

@Service
public class DefaultValidationHubClient implements ValidationHubClient {
    private final boolean hubAvailable;

    public DefaultValidationHubClient(@Value("${validation.hub.available:true}") boolean hubAvailable) {
        this.hubAvailable = hubAvailable;
    }

    @Override
    public void confirmValidation(Billet billet, SegmentBillet segment) {
        if (!hubAvailable) {
            throw new ValidationUnavailableException("Hub unavailable");
        }
    }
}
