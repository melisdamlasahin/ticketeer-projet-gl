package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ticket_train.ticketeer.dto.mobile.AchatBilletRequest;
import ticket_train.ticketeer.dto.mobile.AchatBilletResponse;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Ville;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.ClientRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchatBilletServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ServiceFerroviaireRepository serviceFerroviaireRepository;
    @Mock
    private BilletRepository billetRepository;
    @Mock
    private SegmentBilletRepository segmentBilletRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClientTokenService clientTokenService;
    @Mock
    private SignedQrService signedQrService;

    private MobileApiService mobileApiService;
    private UUID clientId;
    private UUID serviceId;
    private UUID returnServiceId;

    @BeforeEach
    void setUp() {
        mobileApiService = new MobileApiService(
                clientRepository,
                serviceFerroviaireRepository,
                billetRepository,
                segmentBilletRepository,
                passwordEncoder,
                clientTokenService,
                signedQrService
        );
        clientId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        returnServiceId = UUID.randomUUID();
    }

    @Test
    void confirmerAchatCreatesTicketForAuthorizedClient() {
        Client client = new Client("Jean", "Dupont", "jean@test", "hash", "photo");
        client.setClientId(clientId);

        ServiceFerroviaire service = new ServiceFerroviaire(
                LocalDate.of(2026, 3, 18),
                new Train("T1", "TGV Test"),
                new Ville("Paris"),
                new Ville("Lyon"),
                100.0
        );
        service.setServiceId(serviceId);

        when(clientTokenService.requireAuthenticatedClientId()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceFerroviaireRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(billetRepository.existsByCodeOptique(any())).thenReturn(false);
        doAnswer(invocation -> {
            Billet billet = invocation.getArgument(0);
            if (billet.getTicketId() == null) {
                billet.setTicketId(UUID.randomUUID());
            }
            return billet;
        }).when(billetRepository).save(any(Billet.class));

        AchatBilletRequest request = new AchatBilletRequest();
        request.setClientId(clientId.toString());
        request.setServiceId(serviceId.toString());
        request.setProfilTarifaire("STANDARD");

        AchatBilletResponse response = mobileApiService.confirmerAchat(request);

        assertTrue(response.getSuccess());
        assertEquals(100.0, response.getPrixFinal());
    }

    @Test
    void confirmerAchatCreatesRoundTripTicketWithTwoSegments() {
        Client client = new Client("Jean", "Dupont", "jean@test", "hash", "photo");
        client.setClientId(clientId);

        ServiceFerroviaire outboundService = new ServiceFerroviaire(
                LocalDate.of(2026, 3, 18),
                new Train("T1", "TGV Test"),
                new Ville("Paris"),
                new Ville("Lyon"),
                100.0
        );
        outboundService.setServiceId(serviceId);

        ServiceFerroviaire returnService = new ServiceFerroviaire(
                LocalDate.of(2026, 3, 21),
                new Train("T2", "TGV Retour"),
                new Ville("Lyon"),
                new Ville("Paris"),
                80.0
        );
        returnService.setServiceId(returnServiceId);

        when(clientTokenService.requireAuthenticatedClientId()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceFerroviaireRepository.findById(serviceId)).thenReturn(Optional.of(outboundService));
        when(serviceFerroviaireRepository.findById(returnServiceId)).thenReturn(Optional.of(returnService));
        when(billetRepository.existsByCodeOptique(any())).thenReturn(false);
        doAnswer(invocation -> {
            Billet billet = invocation.getArgument(0);
            if (billet.getTicketId() == null) {
                billet.setTicketId(UUID.randomUUID());
            }
            return billet;
        }).when(billetRepository).save(any(Billet.class));

        AchatBilletRequest request = new AchatBilletRequest();
        request.setClientId(clientId.toString());
        request.setServiceId(serviceId.toString());
        request.setReturnServiceId(returnServiceId.toString());
        request.setProfilTarifaire("STANDARD");

        AchatBilletResponse response = mobileApiService.confirmerAchat(request);

        ArgumentCaptor<Billet> billetCaptor = ArgumentCaptor.forClass(Billet.class);
        verify(billetRepository, org.mockito.Mockito.atLeastOnce()).save(billetCaptor.capture());
        List<Billet> savedBillets = billetCaptor.getAllValues();
        Billet savedBillet = savedBillets.get(savedBillets.size() - 1);

        assertTrue(response.getSuccess());
        assertEquals(180.0, response.getPrixFinal());
        assertNotNull(savedBillet);
        assertEquals(2, savedBillet.getSegments().size());
        assertEquals(1, savedBillet.getSegments().get(0).getOrdre());
        assertEquals(2, savedBillet.getSegments().get(1).getOrdre());
    }
}
