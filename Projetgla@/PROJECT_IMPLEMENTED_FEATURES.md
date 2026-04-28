# Ticketeer Project

## Actually Implemented Features

This document presents the features that are actually implemented in the project, organized around the four main subjects:

1. Data model + database + initial setup
2. Client side: browse services, buy ticket, download ticket
3. Control Unit + controller authentication + validation interface
4. Validation rules + security + anti-fraud + tests + integration

---

## 1. Data Model + Database + Initial Setup

The backend is implemented with Spring Boot and Spring Data JPA. The project contains a complete persistent data model for railway services, clients, tickets, controller accounts, and validation traces.

### Implemented data model

The following persistent entities are implemented:

- [Client.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Client.java)
- [Billet.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Billet.java)
- [SegmentBillet.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/SegmentBillet.java)
- [ServiceFerroviaire.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/ServiceFerroviaire.java)
- [ServiceCheckpoint.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/ServiceCheckpoint.java)
- [Train.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Train.java)
- [Ville.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Ville.java)
- [Controleur.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Controleur.java)
- [Validation.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Validation.java)

### Implemented business enums

- [ProfilTarifaire.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/enums/ProfilTarifaire.java)
- [TicketStatus.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/enums/TicketStatus.java)
- [SegmentStatus.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/enums/SegmentStatus.java)
- [ValidationResult.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/enums/ValidationResult.java)
- [ValidationMotif.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/enums/ValidationMotif.java)

### Implemented repositories

- [ClientRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/ClientRepository.java)
- [BilletRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/BilletRepository.java)
- [SegmentBilletRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/SegmentBilletRepository.java)
- [ServiceFerroviaireRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/ServiceFerroviaireRepository.java)
- [ServiceCheckpointRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/ServiceCheckpointRepository.java)
- [TrainRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/TrainRepository.java)
- [VilleRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/VilleRepository.java)
- [ControleurRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/ControleurRepository.java)
- [ValidationRepository.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/ValidationRepository.java)

### Initial setup and configuration

The application startup and configuration are implemented in:

- [Application.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/Application.java)
- [application.properties](./tickecteer/ticketeer/src/main/resources/application.properties)
- [PasswordConfig.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/PasswordConfig.java)
- [DataInitializer.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/DataInitializer.java)

### Seeded data

The project initializes:

- fixed cities
- fixed trains
- fixed railway services
- service checkpoints
- test clients
- test controllers
- sample tickets

This means the system can start with a preconfigured railway network and test accounts ready for demonstration.

---

## 2. Client Side: Browse Services, Buy Ticket, Download Ticket

The client side is implemented as an Android mobile application connected to the Spring Boot backend through mobile API endpoints.

### Service browsing

The mobile client allows users to browse predefined railway services and search for routes.

Implemented Android screens:

- [WelcomeActivity.java](./app/src/main/java/com/easyrail/app/WelcomeActivity.java)
- [SearchActivity.java](./app/src/main/java/com/easyrail/app/SearchActivity.java)
- [ResultsActivity.java](./app/src/main/java/com/easyrail/app/ResultsActivity.java)

Implemented backend support:

- [MobileApiController.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/MobileApiController.java)
- [MobileApiService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/MobileApiService.java)
- [ServiceResponse.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/mobile/ServiceResponse.java)
- [TarificationService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/TarificationService.java)

### Ticket purchase

The client can:

- choose a railway service
- select a tariff profile
- choose reservation class
- simulate payment
- confirm purchase

Implemented Android purchase flow:

- [ConfirmationActivity.java](./app/src/main/java/com/easyrail/app/ConfirmationActivity.java)
- [SuccessActivity.java](./app/src/main/java/com/easyrail/app/SuccessActivity.java)

Implemented purchase DTOs and models:

- [AchatBilletRequest.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/mobile/AchatBilletRequest.java)
- [AchatBilletResponse.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/mobile/AchatBilletResponse.java)
- [TarificationResponse.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/mobile/TarificationResponse.java)

Implemented purchase/ticket services:

- [MobileTicketService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/MobileTicketService.java)
- [TarificationService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/TarificationService.java)

### QR code and downloadable ticket

After a successful purchase, the system:

- creates the ticket
- creates ticket segments
- generates an optical code
- generates a QR payload
- generates a PDF ticket

Implemented ticket generation features:

- [SignedQrService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/SignedQrService.java)
- [MobileTicketService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/MobileTicketService.java)
- [TicketResponse.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/mobile/TicketResponse.java)

Implemented Android ticket screens:

- [MyTicketsActivity.java](./app/src/main/java/com/easyrail/app/MyTicketsActivity.java)
- [TicketActivity.java](./app/src/main/java/com/easyrail/app/TicketActivity.java)

The mobile client supports:

- ticket listing
- ticket detail display
- QR display
- PDF download
- ticket cancellation
- ticket modification

---

## 3. Control Unit + Controller Authentication + Validation Interface

The controller/control-unit side is implemented as a secured Spring MVC + Thymeleaf web interface.

### Controller authentication

The controller can log in through a secured authentication flow.

Implemented authentication/configuration files:

- [AuthController.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/AuthController.java)
- [ControlUnitAuthService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ControlUnitAuthService.java)
- [SecurityConfig.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/SecurityConfig.java)
- [PasswordConfig.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/PasswordConfig.java)

Implemented authentication template:

- [login.html](./tickecteer/ticketeer/src/main/resources/templates/controleur/login.html)

### Control-unit interface

The web control unit allows the controller to:

- log in
- view the control-unit home page
- choose the current service
- open the scan page
- manually enter a code or use the scan interface

Implemented controller-side files:

- [ControlUnitController.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/ControlUnitController.java)
- [ValidationApiController.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/ValidationApiController.java)
- [ScanBilletForm.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/ScanBilletForm.java)
- [ValidationRequest.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/ValidationRequest.java)
- [ValidationResponse.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/ValidationResponse.java)

Implemented templates:

- [home.html](./tickecteer/ticketeer/src/main/resources/templates/controleur/home.html)
- [scan.html](./tickecteer/ticketeer/src/main/resources/templates/controleur/scan.html)

Implemented frontend assets:

- [main.css](./tickecteer/ticketeer/src/main/resources/static/css/main.css)
- [app.js](./tickecteer/ticketeer/src/main/resources/static/js/app.js)

### Validation interface behavior

The implemented interface supports:

- current service selection
- checkpoint selection
- optical code submission
- transmission of validation request to the backend
- display of validation result and motif on screen

---

## 4. Validation Rules + Security + Anti-Fraud + Tests + Integration

This is the core logic of the project. It covers validation decisions, fraud detection, security controls, rate limiting, audit logging, and backend testing.

### Core validation logic

Implemented in:

- [ValidationService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ValidationService.java)
- [ValidationTraceService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ValidationTraceService.java)

The validation logic handles cases such as:

- unknown ticket
- unreadable code
- invalid QR signature
- wrong service
- already validated ticket
- validation before allowed checkpoint
- validation after allowed route
- validation after journey completion
- temporary validation impossibility

### Anti-fraud logic

Implemented in:

- [FraudDetectionService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/FraudDetectionService.java)

The anti-fraud implementation includes:

- replay detection
- duplicate validation prevention
- backtracking detection
- suspicious controller validation velocity
- cross-service anomaly detection
- journey-time abuse detection
- checkpoint chronology checks

### Security implementation

Implemented security components:

- [SecurityConfig.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/SecurityConfig.java)
- [MobileApiAuthenticationFilter.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/security/MobileApiAuthenticationFilter.java)
- [ClientTokenService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ClientTokenService.java)
- [LoginAttemptService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/LoginAttemptService.java)
- [ControllerValidationRateLimitService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ControllerValidationRateLimitService.java)
- [SecurityAuditService.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/SecurityAuditService.java)
- [GlobalExceptionHandler.java](./tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/GlobalExceptionHandler.java)

The implemented security features include:

- controller authentication with Spring Security
- secured controller-side access
- protected mobile API endpoints
- signed mobile auth tokens
- token-based authorization
- logout token revocation
- login rate limiting
- controller validation endpoint throttling
- audit logging for auth and abuse events
- structured API error handling

### Android-side secure session handling

Implemented in:

- [SessionManager.java](./app/src/main/java/com/easyrail/app/SessionManager.java)
- [ApiSessionHandler.java](./app/src/main/java/com/easyrail/app/ApiSessionHandler.java)
- [ProfileActivity.java](./app/src/main/java/com/easyrail/app/ProfileActivity.java)
- [MyTicketsActivity.java](./app/src/main/java/com/easyrail/app/MyTicketsActivity.java)
- [TicketActivity.java](./app/src/main/java/com/easyrail/app/TicketActivity.java)
- [ConfirmationActivity.java](./app/src/main/java/com/easyrail/app/ConfirmationActivity.java)

Implemented mobile-side protections include:

- stale token detection
- forced re-login on unauthorized responses
- session migration support
- backend logout call before local logout

### Tests and integration

Implemented backend tests:

- [ApplicationTests.java](./tickecteer/ticketeer/src/test/java/ticket_train/tickecteer/ApplicationTests.java)
- [ValidationServiceTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/ValidationServiceTest.java)
- [AchatBilletServiceTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/AchatBilletServiceTest.java)
- [FraudDetectionServiceTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/FraudDetectionServiceTest.java)
- [MobileTicketServiceTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/MobileTicketServiceTest.java)
- [SignedQrServiceTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/SignedQrServiceTest.java)
- [ClientTokenServiceTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/ClientTokenServiceTest.java)
- [ValidationApiIntegrationTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/ValidationApiIntegrationTest.java)
- [SecurityIntegrationTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/SecurityIntegrationTest.java)
- [MobileApiValidationIntegrationTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/MobileApiValidationIntegrationTest.java)
- [MobileApiAuthLifecycleIntegrationTest.java](./tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/MobileApiAuthLifecycleIntegrationTest.java)

Implemented Android test:

- [ApiSessionHandlerTest.java](./app/src/test/java/com/easyrail/app/ApiSessionHandlerTest.java)

The current tested behaviors include:

- validation decisions and motifs
- fraud detection cases
- signed QR compatibility and verification
- ticket generation and PDF generation
- mobile auth lifecycle
- stale/invalid token rejection
- forbidden profile access
- forbidden ticket access
- forbidden PDF access
- mobile validation request validation
- secured controller endpoint access
- controller endpoint abuse throttling
- login rate limiting
- logout token revocation

---

## Final Implemented Functional Scope

The current project actually supports:

- persistent railway data model and repositories
- seeded railway network and test accounts
- Android mobile client for passenger-side flows
- service browsing
- tariff calculation
- simulated purchase flow
- ticket creation with segments
- QR generation and signed QR validation
- PDF ticket generation and download
- ticket listing, consultation, modification, and cancellation
- secured controller login
- controller scan/validation interface
- deterministic validation with motifs
- validation trace persistence
- anti-fraud checks
- Spring Security integration
- mobile token authentication
- logout revocation
- rate limiting
- audit logging
- unit and integration tests
