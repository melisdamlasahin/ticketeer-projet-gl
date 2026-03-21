package ticket_train.ticketeer.service;

import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.SegmentBillet;

public interface ValidationHubClient {
    void confirmValidation(Billet billet, SegmentBillet segment);
}
