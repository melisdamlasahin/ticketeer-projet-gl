package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ticket_train.ticketeer.dto.mobile.AchatBilletRequest;
import ticket_train.ticketeer.dto.mobile.TicketResponse;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MobileTicketServiceTest {

    @Mock
    private BilletRepository billetRepository;
    @Mock
    private SegmentBilletRepository segmentBilletRepository;
    @Mock
    private SignedQrService signedQrService;

    private MobileTicketService mobileTicketService;

    @BeforeEach
    void setUp() {
        mobileTicketService = new MobileTicketService(
                billetRepository,
                segmentBilletRepository,
                signedQrService,
                new TarificationService()
        );
    }

    @Test
    void buildTicketPdfReturnsPdfDocument() {
        TicketResponse response = new TicketResponse();
        response.setBilletId("ticket-123");
        response.setVilleDepartNom("Paris");
        response.setVilleArriveeNom("Lyon");
        response.setTrainNom("TGV Test");
        response.setClasseReservation("SECONDE");
        response.setNumeroPlace("B-12");
        response.setDateTrajet("2026-05-03");
        response.setHeureDepart("07:15");
        response.setVoie("12");
        response.setPrixFinal(49.90);

        byte[] pdf = mobileTicketService.buildTicketPdf(response);

        assertTrue(pdf.length > 20);
        assertArrayEquals("%PDF-1.4".getBytes(), java.util.Arrays.copyOf(pdf, 8));
    }

    @Test
    void updateTicketPreservesExistingSeatWhenNoPreferenceProvided() {
        Client client = new Client("Jean", "Dupont", "jean@test", "hash", "photo");
        Billet billet = new Billet("TICK-001", BigDecimal.TEN, client);
        billet.setTicketId(UUID.randomUUID());
        billet.setEtat(TicketStatus.DISPONIBLE);
        billet.setClasseReservation("SECONDE");
        billet.setNumeroPlace("B-12");
        billet.setNomPassager("Jean Dupont");

        AchatBilletRequest request = new AchatBilletRequest();
        request.setClasseReservation("Seconde");
        request.setNomPassager("Jean Dupont");
        when(signedQrService.buildSignedPayload(billet)).thenReturn("signed-payload");

        mobileTicketService.updateTicket(billet, request);

        assertEquals("B-12", billet.getNumeroPlace());
        verify(billetRepository).save(billet);
    }
}
