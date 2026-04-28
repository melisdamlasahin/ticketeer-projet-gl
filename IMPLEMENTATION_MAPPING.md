# Ticketeer Project

## Actually Implemented Features

This document summarizes the features that are actually implemented in the Ticketeer project. The project is composed of:

- a Spring Boot backend in `tickecteer/ticketeer`
- an Android mobile client in `app`
- a web control-unit interface for controllers

## 1. Data Model + Database + Initial Setup

The backend data model is implemented with JPA entities and Spring Data repositories.

### Core entities

- `Client`: customer account and profile data  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Client.java`
- `Billet`: ticket data, ownership, status, optical code, QR payload, PDF content references  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Billet.java`
- `SegmentBillet`: ticket segments and validation state across a journey  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/SegmentBillet.java`
- `ServiceFerroviaire`: railway service definition with route, departure time, and train  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/ServiceFerroviaire.java`
- `ServiceCheckpoint`: ordered service checkpoints used for validation chronology and anti-fraud checks  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/ServiceCheckpoint.java`
- `Train`: train definition and capacity data  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Train.java`
- `Ville`: supported cities in the railway network  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Ville.java`
- `Controleur`: authenticated controller account for the control unit  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Controleur.java`
- `Validation`: stored validation trace with result, motif, checkpoint, and timestamps  
  File: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Validation.java`

### Business enums

- `ProfilTarifaire`
- `TicketStatus`
- `SegmentStatus`
- `ValidationResult`
- `ValidationMotif`

Files: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/enums/`

### Persistence layer

The project includes Spring Data repositories for all main business entities:

- `ClientRepository`
- `BilletRepository`
- `SegmentBilletRepository`
- `ServiceFerroviaireRepository`
- `ServiceCheckpointRepository`
- `TrainRepository`
- `VilleRepository`
- `ControleurRepository`
- `ValidationRepository`

Files: `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/repository/`

### Initial setup and seeded data

The backend application starts with Spring Boot and is configured through:

- `Application.java`  
  `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/Application.java`
- `application.properties`  
  `tickecteer/ticketeer/src/main/resources/application.properties`
- `DataInitializer.java`  
  `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/DataInitializer.java`

The initializer seeds the application with fixed railway data, including:

- cities
- trains
- railway services
- service checkpoints
- demo clients
- controller accounts
- demo ticket data

### Main implemented result

At startup, the backend provides a ready-to-use railway network with persistent business entities, seeded operational data, and repository access for the rest of the system.

## 2. Client Side: Browse Services, Buy Ticket, Download Ticket

The client side is implemented as an Android application connected to the Spring Boot backend through a mobile API.

### Client authentication and session

The mobile application supports:

- welcome screen
- client registration
- client login
- persistent local session storage
- session migration from older branding
- graceful handling of expired or invalid sessions
- logout with backend invalidation

Main files:

- `app/src/main/java/com/easyrail/app/WelcomeActivity.java`
- `app/src/main/java/com/easyrail/app/RegisterActivity.java`
- `app/src/main/java/com/easyrail/app/LoginActivity.java`
- `app/src/main/java/com/easyrail/app/SessionManager.java`
- `app/src/main/java/com/easyrail/app/ApiSessionHandler.java`

### Browse railway services

The mobile app supports service consultation and search through:

- service search form
- results listing
- API-backed retrieval of available railway services
- tariff quotation before purchase

Main files:

- `app/src/main/java/com/easyrail/app/SearchActivity.java`
- `app/src/main/java/com/easyrail/app/ResultsActivity.java`
- `app/src/main/java/com/easyrail/app/ServiceResultAdapter.java`
- `app/src/main/java/com/easyrail/app/ServiceApiModel.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/MobileApiController.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/MobileApiService.java`

### Buy a ticket

The implemented purchase flow includes:

- selection of a service
- selection of tariff profile
- price calculation
- simulated purchase confirmation
- ticket creation in the database
- segment creation for the selected journey
- seat assignment
- unique optical code generation
- QR payload generation

Main files:

- `app/src/main/java/com/easyrail/app/ConfirmationActivity.java`
- `app/src/main/java/com/easyrail/app/AchatBilletRequest.java`
- `app/src/main/java/com/easyrail/app/AchatBilletResponse.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/TarificationService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/MobileTicketService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/mobile/AchatBilletRequest.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/mobile/AchatBilletResponse.java`

### View and download tickets

The mobile app supports:

- ticket list for the connected client
- ticket detail view
- PDF ticket download
- QR-based digital ticket display
- ticket modification
- ticket cancellation

Main files:

- `app/src/main/java/com/easyrail/app/MyTicketsActivity.java`
- `app/src/main/java/com/easyrail/app/TicketActivity.java`
- `app/src/main/java/com/easyrail/app/MyTicketsAdapter.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/MobileTicketService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/SignedQrService.java`

### Additional client profile features

The mobile application also includes:

- profile viewing
- profile editing
- support screen
- success/confirmation screen after purchase

Main files:

- `app/src/main/java/com/easyrail/app/ProfileActivity.java`
- `app/src/main/java/com/easyrail/app/SupportActivity.java`
- `app/src/main/java/com/easyrail/app/SuccessActivity.java`

### Main implemented result

The client can register, log in, search available services, choose a tariff profile, buy a ticket, access previously purchased tickets, and download a PDF/QR version of the ticket from the Android app.

## 3. Control Unit + Controller Authentication + Validation Interface

The controller side is implemented as a secured web control-unit interface backed by Spring MVC and Spring Security.

### Controller authentication

The control-unit interface includes:

- controller login page
- authenticated session for controllers
- restricted access to control-unit pages

Main files:

- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/AuthController.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ControlUnitAuthService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/SecurityConfig.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/PasswordConfig.java`

### Control-unit interface

The implemented web interface provides:

- login page
- controller home page
- ticket scan/manual validation page

Templates:

- `tickecteer/ticketeer/src/main/resources/templates/controleur/login.html`
- `tickecteer/ticketeer/src/main/resources/templates/controleur/home.html`
- `tickecteer/ticketeer/src/main/resources/templates/controleur/scan.html`

Supporting assets:

- `tickecteer/ticketeer/src/main/resources/static/css/main.css`
- `tickecteer/ticketeer/src/main/resources/static/js/app.js`

### Validation request flow

The control unit supports:

- manual optical-code entry
- selection of the current service
- submission of validation requests to the backend
- display of validation result and motif

Main files:

- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/ControlUnitController.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/ValidationApiController.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/ScanBilletForm.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/ValidationRequest.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/dto/ValidationResponse.java`

### Main implemented result

An authenticated controller can open the control-unit web interface, choose the active service/checkpoint context, enter or scan a ticket code, and receive a validation decision from the backend with an explicit reason.

## 4. Validation Rules + Security + Anti-Fraud + Tests + Integration

This part is implemented in the backend service layer, security layer, and automated test suite.

### Validation rules

The validation engine handles:

- unknown ticket detection
- unreadable or invalid optical/QR payload rejection
- wrong-service detection
- already validated or already used checks
- coherent segment state transitions
- temporary validation impossibility responses
- signed QR verification
- journey and checkpoint chronology checks
- validation trace persistence

Main files:

- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ValidationService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ValidationTraceService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/SignedQrService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/Validation.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/model/ServiceCheckpoint.java`

### Security

The implemented security features include:

- Spring Security configuration for controller pages and API routes
- password hashing configuration
- authenticated controller sessions
- signed mobile authentication tokens
- mobile API authentication filter
- logout token revocation
- unauthorized mobile-session handling in the Android app
- login rate limiting
- controller validation endpoint throttling
- centralized exception handling
- security audit logging

Main files:

- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/SecurityConfig.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/config/PasswordConfig.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/security/MobileApiAuthenticationFilter.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ClientTokenService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/LoginAttemptService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ControllerValidationRateLimitService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/SecurityAuditService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/controller/GlobalExceptionHandler.java`
- `app/src/main/java/com/easyrail/app/ApiSessionHandler.java`

### Anti-fraud

The anti-fraud logic currently checks for:

- replay attempts on the same ticket
- repeated suspicious validation patterns
- checkpoint backtracking
- impossible checkpoint chronology
- controller validation velocity anomalies
- cross-service anomalies
- validation outside the expected journey time window

Main files:

- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/FraudDetectionService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ValidationService.java`
- `tickecteer/ticketeer/src/main/java/ticket_train/ticketeer/service/ValidationTraceService.java`

### Tests and integration

The backend includes unit and integration tests for validation, security, fraud, ticket generation, and mobile API behavior.

Implemented test coverage includes:

- validation service rules
- ticket purchase service behavior
- signed token behavior
- QR signature verification
- PDF ticket generation
- mobile API input validation
- mobile authentication lifecycle
- forbidden profile/ticket/PDF access between clients
- controller security
- validation API integration
- controller endpoint abuse throttling

Main backend test files:

- `tickecteer/ticketeer/src/test/java/ticket_train/tickecteer/ApplicationTests.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/ValidationServiceTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/AchatBilletServiceTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/ClientTokenServiceTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/FraudDetectionServiceTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/MobileTicketServiceTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/service/SignedQrServiceTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/MobileApiValidationIntegrationTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/MobileApiAuthLifecycleIntegrationTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/SecurityIntegrationTest.java`
- `tickecteer/ticketeer/src/test/java/ticket_train/ticketeer/controller/ValidationApiIntegrationTest.java`

Android-side automated coverage includes:

- shared unauthorized-response handling

Main Android test file:

- `app/src/test/java/com/easyrail/app/ApiSessionHandlerTest.java`

### Main implemented result

The system includes deterministic validation logic, controller authentication, mobile API protection, signed QR verification, anti-reuse and anti-fraud controls, and automated tests covering the main validation, purchase, security, and integration flows.
