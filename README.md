# EasyRail

EasyRail is a railway ticketing project with three runtime parts:

- Android passenger app in `app/`
- Spring Boot backend in `tickecteer/ticketeer/`
- Spring Boot web interface for train controllers, served by the backend

The passenger client is Android. The web UI is only for controller authentication and ticket validation.

## Architecture

- `app/`: Android client for registration, login, service browsing, purchase, ticket display, PDF download, and profile management
- `tickecteer/ticketeer/`: central API, ticket generation, QR signing, validation rules, controller login, and controller validation screens
- `mysql-data/`: local MySQL data directory used by the backend in local development

## Run The Project

### 1. Start the backend

From the project root:

```bash
cd tickecteer/ticketeer
sh mvnw spring-boot:run
```

Default backend URL:

```text
http://localhost:8080
```

### 2. Run the Android app

Open the project root in Android Studio:

```text
/Users/melisdamlasahin/IdeaProjects/ticketeer-projet-gl/ticketeer-projet-gl/Projetgla@
```

Then run the `app` module on an emulator.

The default Android API base URL is:

```text
http://10.0.2.2:8080/
```

This is correct for the Android emulator because `10.0.2.2` maps to the host machine.

### 3. Override the Android API base URL

For a physical device or a different backend host, override the URL at build time instead of editing source files:

```bash
sh gradlew installDebug -PEASYRAIL_API_BASE_URL=http://192.168.1.20:8080/
```

## Demo Accounts

Seed data is created by the backend `DataInitializer`.

Controller accounts:

- login: `nathan` / password: `anne123`
- login: `christian` / password: `essome123`

Client accounts created on startup:

- email: `jean.dupont@easyrail.test` / password: `password123`
- email: `marie.martin@easyrail.test` / password: `password123`

## Main Flows

### Android passenger flow

- register or log in
- browse predefined railway services
- calculate fare based on tariff profile
- purchase a ticket
- display ticket details and QR code
- download the ticket as PDF
- list purchased tickets

### Controller validation flow

- log in on the controller web interface
- choose a service
- scan or enter a ticket code
- receive a deterministic validation result

## Backend Tests

Run backend tests with:

```bash
cd tickecteer/ticketeer
sh mvnw test
```

## Current Technical Direction

This repository should be understood as:

- Android-first client architecture
- Spring Boot API backend
- web validation console for train controllers

It is not a passenger-facing Thymeleaf web application.
