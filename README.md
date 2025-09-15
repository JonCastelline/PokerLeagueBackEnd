# Poker League Backend API

This repository contains the backend API for the Poker League mobile application. It provides all the necessary services for user management, league management, game tracking, and real-time game state updates.

## Features

*   **User Authentication & Authorization:** Secure login, registration, and role-based access control.
*   **League Management:** Create, join, update, and manage poker leagues.
*   **Season Management:** Organize games into distinct seasons within a league.
*   **Game Management:** Create, update, delete, and track poker games.
*   **Live Game Engine:** Real-time management of game state, blind timers, player elimination, and results.
*   **Player Standings:** Automatic calculation and display of player standings based on game results.
*   **Security Questions:** Password recovery via security questions.

## Tech Stack

*   **Language:** Kotlin
*   **Framework:** Spring Boot 3.x
*   **Database:** PostgreSQL (local development uses Docker, production uses Render's managed PostgreSQL)
*   **ORM:** Spring Data JPA / Hibernate
*   **Database Migrations:** Liquibase
*   **Security:** Spring Security, JWT (JSON Web Tokens)
*   **API Documentation:** Springdoc OpenAPI (Swagger UI)

## Getting Started

### Prerequisites

Ensure you have the following installed on your system:

*   **Java Development Kit (JDK):** Version 17 or higher.
*   **Docker Desktop:** For running the local PostgreSQL database.
*   **Gradle:** (Optional, as `gradlew` wrapper is included)

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd PokerLeagueBackEnd
    ```
2.  **Start the local PostgreSQL database:**
    Navigate to the `PokerLeagueBackEnd` directory and run:
    ```bash
    docker-compose up -d
    ```
    *   If this is your first time, Docker will download the PostgreSQL image.
    *   If you encounter issues (e.g., "relation already exists"), you might need to reset the database:
        ```bash
        docker-compose down -v
        docker-compose up -d
        ```
3.  **Run the application:**
    The application uses the `dev` profile for local development. From the `PokerLeagueBackEnd` directory, run:
    ```bash
    .\gradlew.bat bootRun --args='--spring.profiles.active=dev'
    ```
    The application should start on `http://localhost:8080`. Liquibase will automatically apply database migrations on startup.

## Running Tests

To run the backend unit and integration tests, navigate to the `PokerLeagueBackEnd` directory and execute:
```bash
.\gradlew.bat test
```

## API Documentation

The interactive API documentation (Swagger UI) is available when the application is running locally:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

This documentation provides details on all available endpoints, their parameters, and expected responses.
