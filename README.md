# Ticketeer
<img width="1672" height="941" alt="369935e4-4116-41a7-8056-1b102ebae7de" src="https://github.com/user-attachments/assets/f98d284e-d3d8-4331-bf22-048bf21ec2eb" />

Ticketeer is a railway ticketing project with three runtime parts:

- Android passenger app in `app/`
<img width="240" height="494" alt="Screenshot 2026-07-03 at 19 19 32" src="https://github.com/user-attachments/assets/55fa4631-1e6f-4fed-82c3-d0e7e52df981" />
<img width="240" height="494" alt="Screenshot 2026-07-03 at 19 18 33" src="https://github.com/user-attachments/assets/5f95c951-a187-4814-8063-b9b68c701283" />
<img width="240" height="494" alt="Screenshot 2026-07-03 at 19 19 38" src="https://github.com/user-attachments/assets/26247bb1-637d-4d3d-a06b-22f409f9339e" />

- Spring Boot backend in `tickecteer/ticketeer/`
- Spring Boot web interface for train controllers, served by the backend
<img width="653" height="487" alt="Screenshot 2026-07-03 at 19 24 00" src="https://github.com/user-attachments/assets/eabe15cb-038d-4e1b-9128-f9265e2902cd" />
<img width="569" height="426" alt="Screenshot 2026-07-03 at 19 23 44" src="https://github.com/user-attachments/assets/57a82b0e-9f54-4cca-a381-0b1a9830e02e" />

The passenger client is Android. The web UI is only for controller authentication and ticket validation.

---

## Architecture

```text
ticketeer-projet-gl/
├── app/                    # Android passenger client
├── tickecteer/ticketeer/   # Spring Boot backend and controller web interface
├── mysql-data/             # Local MySQL data directory for development
└── README.md
```

## Android Passenger App

The Android application allows passengers to:

* Register and log in
* Browse predefined railway services
* Calculate ticket fares based on tariff profiles
* Purchase tickets
* Display ticket details and QR codes
* Download tickets as PDF files
* Manage their profile
* View purchased tickets

---

## Spring Boot Backend

The backend is responsible for:

* User authentication
* Railway service management
* Ticket generation
* QR code signing
* Ticket validation rules
* Controller authentication
* Serving the controller web interface

---

## Controller Web Interface

The controller interface allows train controllers to:

* Log in securely
* Select a railway service
* Scan or manually enter a ticket code
* Validate tickets
* Receive a deterministic validation result

---

## Technologies Used

### Backend

* Java
* Spring Boot
* Spring MVC
* Spring Data JPA
* MySQL
* Maven

### Android

* Android SDK
* Java / Kotlin
* Gradle
* REST API integration
* QR code display
* PDF ticket download

### Other

* MySQL local data storage
* Web interface for ticket validation
* Automated backend seed data through `DataInitializer`

---

## Run the Project

### 1. Start the Backend

From the project root:

```bash
cd tickecteer/ticketeer
sh mvnw spring-boot:run
```

Default backend URL:

```text
http://localhost:8080
```

---

### 2. Run the Android App

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

---

### 3. Override the Android API Base URL

For a physical device or a different backend host, override the URL at build time instead of editing source files:

```bash
sh gradlew installDebug -PEASYRAIL_API_BASE_URL=http://192.168.1.20:8080/
```

---

## Demo Accounts

Seed data is created automatically by the backend through `DataInitializer`.

### Controller Accounts

| Login       | Password    |
| ----------- | ----------- |
| `nathan`    | `anne123`   |
| `christian` | `essome123` |

### Passenger Accounts

| Email                        | Password      |
| ---------------------------- | ------------- |
| `jean.dupont@easyrail.test`  | `password123` |
| `marie.martin@easyrail.test` | `password123` |

---

## Main Flows

### Android Passenger Flow

1. Register or log in
2. Browse predefined railway services
3. Calculate fare based on tariff profile
4. Purchase a ticket
5. Display ticket details and QR code
6. Download the ticket as a PDF
7. View purchased tickets

### Controller Validation Flow

1. Log in on the controller web interface
2. Choose a railway service
3. Scan or manually enter a ticket code
4. Receive a deterministic validation result
5. Accept or reject the ticket based on the validation status

---

## Backend Tests

Run backend tests with:

```bash
cd tickecteer/ticketeer
sh mvnw test
```

---

## Current Technical Direction

This repository should be understood as an **Android-first railway ticketing project** supported by a centralized **Spring Boot backend**.

The web interface is not a passenger-facing website. It is a dedicated validation console for train controllers.

---

## Repository Purpose

This repository demonstrates:

* Full-stack software engineering with Android and Spring Boot
* REST API design
* User authentication
* Ticket generation and validation
* QR-based ticket control
* PDF ticket export
* Separation between passenger and controller interfaces
* Collaborative software development in an academic project context

---

## Project Context & Credits

This project was completed at **Université Paris-Saclay** as part of the **Projet Génie Logiciel** course.

### Team Members

* **Melis Damla Şahin**
* **Christian Essome Ndoumin**
* **Wang Haoyu**
* **Morel Talekeudjeu Goudjou**

### Supervisor

* **Burkhart Wolff**
