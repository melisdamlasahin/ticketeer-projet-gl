package ticket_train.ticketeer.service;

public class ValidationUnavailableException extends RuntimeException {
    public ValidationUnavailableException(String message) {
        super(message);
    }
}
