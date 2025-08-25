# GeekDiary Android App

A modern Android diary application built with Jetpack Compose, following Material Design 3 guidelines.

## Features

- **Main Page**: View today's diary entry with date navigation
- **Offline-First**: Local storage with background synchronization
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Secure Authentication**: JWT-based authentication with secure token storage

## Tech Stack

- **UI**: Jetpack Compose, Material Design 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **JSON**: Moshi
- **Testing**: JUnit, Mockito, Turbine

## Project Structure

```
app/src/main/java/com/example/geekdiary/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # API services, DTOs
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic use cases
├── presentation/
│   ├── ui/
│   │   ├── main/       # Main screen
│   │   ├── auth/       # Authentication screens
│   │   └── theme/      # App theme
│   └── viewmodel/      # ViewModels
└── di/                 # Hilt modules
```

## Backend API

The app connects to a local backend running at `http://172.18.0.1:8080` with the following endpoints:

- `POST /v1/authorize` - Authentication
- `GET /v1/user` - User profile
- `GET /v1/items` - Get diary entries
- `PUT /v1/items` - Create/update diary entries
- `GET /v1/sync/changes` - Synchronization

## Development Setup

1. Clone the repository
2. Open in Android Studio
3. Ensure backend is running at `http://172.18.0.1:8080`
4. Build and run the app

## Testing

Run tests with:
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Code Style

- Follow standard Kotlin conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions small and focused
- Use dependency injection for testability
