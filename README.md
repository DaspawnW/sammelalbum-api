# Sammelalbum - Sticker Trading Platform

A full-stack web application for managing and trading sticker collections. Users can track their sticker collections, search for missing stickers, create offers, and exchange stickers with other collectors.

## 🏗️ Architecture

The project consists of three main components:

- **Backend**: Spring Boot REST API (Java 17)
- **Frontend**: Angular 21 SPA with TailwindCSS
- **E2E Tests**: Playwright + Gauge test automation

## 🚀 Prerequisites

- **Java**: JDK 17 or higher
- **Node.js**: v18 or higher
- **npm**: v10 or higher
- **PostgreSQL**: v14 or higher (for production)
- **Maven**: 3.8+ (or use included Maven wrapper)

## 📦 Project Structure

```
sammelalbum/
├── backend/          # Spring Boot application
├── frontend/         # Angular application
├── e2e/             # End-to-end tests
├── caddy/           # Reverse proxy configuration
└── docker-compose.yml
```

## 🛠️ Local Development

### Backend

The backend is a Spring Boot application using PostgreSQL for production and H2 for testing.

#### Running the Backend

```bash
cd backend

# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or with installed Maven
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

#### Backend Configuration

- **Development**: Uses H2 in-memory database by default
- **Production**: Configure PostgreSQL connection in `application.properties`
- **Test Data**: Automatically loaded from `src/main/resources/test-data.sql` in dev mode

#### Running Backend Tests

```bash
cd backend

# Run all tests
./mvnw test

# Run tests with coverage
./mvnw clean test -Pcoverage

# View coverage report
open target/site/jacoco/index.html
```

**Coverage Metrics:**
- Instructions: 87%
- Lines: 90%
- Methods: 80%
- Classes: 88%

### Frontend

The frontend is an Angular 21 application with TailwindCSS for styling.

#### Running the Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on `http://localhost:4200`

#### Frontend Configuration

- **API Endpoint**: Configured in `src/environments/environment.ts`
- **Default**: Points to `http://localhost:8080`

#### Running Frontend Tests

```bash
cd frontend

# Run all tests
npm test

# Run tests with coverage
npm test -- --coverage

# View coverage report
open coverage/frontend/index.html
```

### E2E Tests

End-to-end tests using Playwright and Gauge to test the complete application flow.

#### Prerequisites for E2E Tests

```bash
cd e2e

# Install dependencies
npm install

# Install Playwright browsers
npx playwright install
```

#### Running E2E Tests

**Important**: Both backend and frontend must be running before executing E2E tests.

```bash
# Terminal 1: Start backend
cd backend && ./mvnw spring-boot:run

# Terminal 2: Start frontend
cd frontend && npm start

# Terminal 3: Run E2E tests
cd e2e

# Run all specs
npm run gauge -- run specs/

# Run specific spec
npm run gauge -- run specs/matches.spec

# View HTML report
open reports/html-report/index.html
```

#### Available E2E Test Specs

- `exchange.spec` - Exchange request workflows
- `full_workflow.spec` - Complete user journey
- `matches.spec` - Match finding and filtering
- `offers.spec` - Offer creation and management
- `search.spec` - Search functionality

## 🧪 Running All Tests

To verify the entire application:

```bash
# 1. Backend tests
cd backend && ./mvnw clean test -Pcoverage

# 2. Frontend tests
cd frontend && npm test -- --coverage

# 3. E2E tests (requires backend + frontend running)
cd e2e && npm run gauge -- run specs/
```

## 🔑 Key Features

- **User Authentication**: JWT-based authentication
- **Sticker Management**: Track owned and needed stickers
- **Offer System**: Create exchange, freebie, or paid offers
- **Match Finding**: Automatic matching of complementary offers
- **Exchange Workflow**: Complete exchange request lifecycle
- **Email Notifications**: Async email notifications for exchanges
- **Reservation System**: Prevent double-booking of stickers

## 🗄️ Database Schema

The application uses the following main entities:

- **Users**: User accounts and credentials
- **Stickers**: Sticker catalog
- **CardOffers**: Stickers offered by users
- **CardSearches**: Stickers needed by users
- **ExchangeRequests**: Exchange transactions between users
- **EmailOutbox**: Queued email notifications

## 📝 API Documentation

When running the backend, API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

## 🐳 Docker Deployment

```bash
# Build and run with Docker Compose
docker-compose -f docker-compose.prod.yml up -d
```

## 🤝 Contributing

1. Ensure all tests pass before submitting changes
2. Maintain test coverage above 80% for backend
3. Follow existing code style and conventions
4. Update E2E tests for new features

## 📄 License

[Add your license here]

## 👥 Authors

[Add authors here]
