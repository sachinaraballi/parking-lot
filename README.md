# Parking Lot Management System

A production-grade REST API for a multi-floor parking lot, built with **Java 17 + Spring Boot 3.2**.  
Persistence is a single JSON file (`data/parking_lot.json`) — no database required.

---

## Table of Contents
1. [Tech Stack](#tech-stack)
2. [Design Principles (SOLID)](#design-principles-solid)
3. [Design Patterns](#design-patterns)
4. [High Level Design (HLD)](#high-level-design-hld)
5. [Low Level Design (LLD)](#low-level-design-lld)
6. [Flow Diagrams](#flow-diagrams)
7. [Data Model](#data-model)
8. [API Endpoints](#api-endpoints)
9. [How to Run](#how-to-run)
10. [Testing with cURL](#testing-with-curl)

---

## Tech Stack

| Layer       | Technology                       |
|-------------|----------------------------------|
| Language    | Java 17                          |
| Framework   | Spring Boot 3.2                  |
| Serialization | Jackson + JavaTimeModule       |
| Build Tool  | Maven                            |
| Database    | JSON file (`data/parking_lot.json`) |
| Validation  | Jakarta Bean Validation          |

---

## Design Principles (SOLID)

### S — Single Responsibility Principle
Each class has exactly one reason to change:

- `ParkingLotRepository` — only reads/writes the JSON file
- `PricingStrategyFactory` — only resolves the correct strategy
- `ParkingService` — only orchestrates business logic
- `ParkingController` / `AdminController` — only handle HTTP concerns

### O — Open/Closed Principle
The `PricingStrategy` interface is **open for extension** (add `ElectricVehiclePricingStrategy`) **without modifying** any existing class. Just implement the interface and register it in `PricingStrategyFactory`.

```java
// Adding a new vehicle type requires zero changes to existing code
public class ElectricVehiclePricingStrategy implements PricingStrategy { ... }
```

### L — Liskov Substitution Principle
`MotorcyclePricingStrategy`, `CarPricingStrategy`, and `TruckPricingStrategy` are fully interchangeable through the `PricingStrategy` interface. Any caller using the interface works correctly with any implementation.

### I — Interface Segregation Principle
`PricingStrategy` is a narrow, focused interface with only the methods callers actually need (`calculate`, `getHourlyRate`, `getDescription`). No fat interfaces that force classes to implement unused methods.

### D — Dependency Inversion Principle
`ParkingService` depends on the `PricingStrategy` **interface**, not on `CarPricingStrategy` directly. High-level policy (when to charge) is decoupled from low-level detail (how much per hour).

```
ParkingService ──depends on──► PricingStrategy (interface)
                                      ▲
               MotorcyclePricingStrategy | CarPricingStrategy | TruckPricingStrategy
```

---

## Design Patterns

| Pattern        | Where Used                             | Purpose                                                      |
|----------------|----------------------------------------|--------------------------------------------------------------|
| **Strategy**   | `PricingStrategy` + implementations   | Swap pricing rules per vehicle type at runtime              |
| **Factory**    | `PricingStrategyFactory`               | Decouple strategy creation from usage                       |
| **Repository** | `ParkingLotRepository`                 | Abstract file I/O behind a domain-friendly interface        |
| **Singleton**  | All Spring `@Service`, `@Repository`   | One shared instance manages state consistently              |
| **Template Method** | `PricingStrategy.calculate()`    | Shared formula skeleton (ceil hours × rate) per subclass    |

---

## High Level Design (HLD)

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client                                  │
│                  (curl / Postman / Frontend)                      │
└──────────────────────────┬──────────────────────────────────────┘
                           │  HTTP / JSON  (port 8080)
┌──────────────────────────▼──────────────────────────────────────┐
│                     Controller Layer                              │
│                                                                   │
│   ParkingController            AdminController                    │
│   POST /api/parking/entry      GET  /api/admin/parking-lot        │
│   POST /api/parking/exit/:id   POST /api/admin/floors             │
│   GET  /api/parking/status     POST /api/admin/floors/:id/spots   │
│   GET  /api/parking/ticket/:id                                    │
│   GET  /api/parking/active-tickets                                │
│   GET  /api/parking/pricing                                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                      Service Layer                                │
│                                                                   │
│                      ParkingService                               │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  park vehicle │ exit vehicle │ get status │ admin ops      │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────┬─────────────────────────────────┬────────────────────┘
           │                                 │
┌──────────▼──────────┐          ┌───────────▼────────────────────┐
│   Strategy Layer    │          │      Repository Layer            │
│                     │          │                                  │
│ PricingStrategyFact │          │   ParkingLotRepository           │
│ ──────────────────  │          │   ─────────────────────────────  │
│ MOTORCYCLE → $1/hr  │          │   load()  → reads JSON file     │
│ CAR        → $2/hr  │          │   save()  → writes JSON file    │
│ TRUCK      → $3.5/hr│          │   (synchronized for thread      │
└─────────────────────┘          │    safety)                       │
                                 └───────────┬────────────────────┘
                                             │
                                 ┌───────────▼────────────────────┐
                                 │      File Database               │
                                 │  data/parking_lot.json           │
                                 │  (auto-created on first start)   │
                                 └────────────────────────────────┘
```

**Default Parking Lot (auto-seeded)**

| Floor | Spot Types                  | Suitable For           |
|-------|-----------------------------|------------------------|
| 1     | 5× SMALL + 5× MEDIUM        | Motorcycles, Cars       |
| 2     | 5× MEDIUM + 5× LARGE        | Cars, Trucks            |
| 3     | 10× LARGE                   | Trucks (and any others) |

---

## Low Level Design (LLD)

### Class Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Model Layer                               │
│                                                                   │
│  ParkingLotData                                                   │
│  ─────────────────                                                │
│  id: String                                                       │
│  name: String              ◄─── has many ───►  ParkingFloor      │
│  address: String                               ──────────────    │
│  floors: List<Floor>                           id: String        │
│  tickets: List<Ticket>     ◄─── has many ───►  floorNumber: int  │
│                                                spots: List       │
│  findFloorWithAvailableSpot()                                     │
│  findTicketById()                   ◄─── has many ───►  ParkingSpot
│  findSpotById()                                          ──────────
│  getTotalSpots()                                         id: String
│  getAvailableSpots()                                     spotNumber
│  getOccupiedSpots()                                      type: SpotType (SMALL/MEDIUM/LARGE)
│                                                          occupied: boolean
│  Ticket                                                  vehicleId: String
│  ─────────────────                                       canFitVehicle(VehicleType)
│  id: String (TKT-XXXXXXXX)
│  vehicle: Vehicle          ◄─── contains ───►  Vehicle
│  spotId: String                                ────────────────
│  floorId: String                               id: String
│  floorNumber: int                              licensePlate: String
│  spotNumber: String                            type: VehicleType
│  spotType: SpotType                                (MOTORCYCLE/CAR/TRUCK)
│  entryTime: LocalDateTime
│  exitTime: LocalDateTime
│  status: TicketStatus (ACTIVE/PAID)
│  amount: Double
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      Strategy Layer                              │
│                                                                   │
│  <<interface>>                                                    │
│  PricingStrategy                                                  │
│  ─────────────────────────────────                               │
│  calculate(durationMinutes): double                               │
│  getHourlyRate(): double                                          │
│  getDescription(): String                                         │
│           ▲                ▲                ▲                     │
│           │                │                │                     │
│  Motorcycle          Car           Truck                          │
│  Pricing             Pricing       Pricing                        │
│  Strategy            Strategy      Strategy                       │
│  ($1.00/hr)          ($2.00/hr)    ($3.50/hr)                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Spot Compatibility Matrix                      │
│                                                                   │
│  Vehicle Type  │  SMALL spot  │  MEDIUM spot  │  LARGE spot      │
│  ──────────────┼──────────────┼───────────────┼──────────────    │
│  MOTORCYCLE    │     YES      │     YES        │     YES          │
│  CAR           │     NO       │     YES        │     YES          │
│  TRUCK         │     NO       │     NO         │     YES          │
│                                                                   │
│  Allocation strategy: always pick the SMALLEST suitable spot     │
│  to maximize space utilization.                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     Package Structure                             │
│                                                                   │
│  com.parkinglot                                                   │
│  ├── ParkingLotApplication.java                                   │
│  ├── config/                                                      │
│  │   └── JacksonConfig.java          (ObjectMapper bean)         │
│  ├── model/                                                       │
│  │   ├── VehicleType.java            (enum)                       │
│  │   ├── SpotType.java               (enum)                       │
│  │   ├── TicketStatus.java           (enum)                       │
│  │   ├── Vehicle.java                                             │
│  │   ├── ParkingSpot.java                                         │
│  │   ├── ParkingFloor.java                                        │
│  │   ├── Ticket.java                                              │
│  │   └── ParkingLotData.java                                      │
│  ├── strategy/                                                    │
│  │   ├── PricingStrategy.java        (interface)                  │
│  │   ├── MotorcyclePricingStrategy.java                           │
│  │   ├── CarPricingStrategy.java                                  │
│  │   └── TruckPricingStrategy.java                                │
│  ├── factory/                                                     │
│  │   └── PricingStrategyFactory.java                              │
│  ├── repository/                                                  │
│  │   └── ParkingLotRepository.java   (file I/O)                   │
│  ├── service/                                                     │
│  │   └── ParkingService.java         (business logic)             │
│  ├── controller/                                                  │
│  │   ├── ParkingController.java                                   │
│  │   └── AdminController.java                                     │
│  ├── dto/                                                         │
│  │   ├── request/                                                 │
│  │   │   ├── ParkVehicleRequest.java                              │
│  │   │   ├── AddFloorRequest.java                                 │
│  │   │   └── AddSpotRequest.java                                  │
│  │   └── response/                                                │
│  │       └── ApiResponse.java                                     │
│  └── exception/                                                   │
│      ├── ParkingException.java                                    │
│      ├── NoSpotAvailableException.java                            │
│      ├── TicketNotFoundException.java                             │
│      └── GlobalExceptionHandler.java                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Flow Diagrams

### 1. Park Vehicle (`POST /api/parking/entry`)

```
Client sends: { licensePlate, vehicleType }
          │
          ▼
  Validate request (Bean Validation)
          │
     INVALID ──────────────────────► 400 Bad Request
          │ VALID
          ▼
  Is vehicle already parked? (scan active tickets)
          │
      YES ────────────────────────► 400 "Vehicle already parked"
          │ NO
          ▼
  Scan floors for available spot compatible with vehicleType
          │
     NONE ──────────────────────── ► 409 "No spot available"
          │ FOUND
          ▼
  Pick SMALLEST suitable spot (MOTORCYCLE prefers SMALL over LARGE)
          │
          ▼
  Create Vehicle (UUID)
  Create Ticket (TKT-XXXXXXXX, entryTime = now, status = ACTIVE)
          │
          ▼
  Mark spot as occupied, link vehicleId
          │
          ▼
  Save updated state to parking_lot.json
          │
          ▼
  Return 201 Created
  {
    "success": true,
    "message": "Vehicle parked successfully.",
    "data": { ticketId, vehicle, floorNumber, spotNumber, entryTime, status }
  }
```

### 2. Exit Vehicle (`POST /api/parking/exit/{ticketId}`)

```
Client sends: ticketId in URL path
          │
          ▼
  Find ticket by ID
          │
    NOT FOUND ─────────────────── ► 404 Ticket not found
          │ FOUND
          ▼
  Is ticket already PAID?
          │
      YES ─────────────────────── ► 400 "Already settled"
          │ NO (ACTIVE)
          ▼
  exitTime = now()
  durationMinutes = exitTime − entryTime  (min 1 minute)
          │
          ▼
  Lookup PricingStrategy via PricingStrategyFactory
  (MOTORCYCLE → $1/hr, CAR → $2/hr, TRUCK → $3.50/hr)
          │
          ▼
  amount = ceil(durationMinutes / 60) × hourlyRate
          │
          ▼
  Update ticket: status=PAID, exitTime, amount
          │
          ▼
  Free the parking spot (occupied=false, vehicleId=null)
          │
          ▼
  Save updated state to parking_lot.json
          │
          ▼
  Return 200 OK
  {
    "ticketId", "licensePlate", "vehicleType",
    "durationMinutes", "pricingRate", "amountDue": "$4.00"
  }
```

### 3. Admin: Add Floor + Spot

```
POST /api/admin/floors  { floorNumber: 4 }
          │
          ▼
  Does floor 4 already exist?
      YES ──────────────────────► 400 "Floor already exists"
          │ NO
          ▼
  Create ParkingFloor(4), add to lot, re-sort by number
  Save → 201 Created { floorId, floorNumber, spots: [] }

          │
          ▼
POST /api/admin/floors/{floorId}/spots  { spotNumber: "F4-L1", spotType: "LARGE" }
          │
          ▼
  Does spot F4-L1 already exist on this floor?
      YES ──────────────────────► 400 "Spot already exists"
          │ NO
          ▼
  Create ParkingSpot, add to floor
  Save → 201 Created { spotId, spotNumber, type, occupied: false }
```

---

## Data Model

The entire system state lives in `data/parking_lot.json`:

```json
{
  "id": "PL-001",
  "name": "Central Parking",
  "address": "123 Main Street, Downtown",
  "floors": [
    {
      "id": "uuid-...",
      "floorNumber": 1,
      "spots": [
        {
          "id": "uuid-...",
          "spotNumber": "F1-S1",
          "type": "SMALL",
          "occupied": false,
          "vehicleId": null
        }
      ]
    }
  ],
  "tickets": [
    {
      "id": "TKT-ABC12345",
      "vehicle": {
        "id": "uuid-...",
        "licensePlate": "MH12AB1234",
        "type": "CAR"
      },
      "spotId": "uuid-...",
      "floorId": "uuid-...",
      "floorNumber": 1,
      "spotNumber": "F1-M1",
      "spotType": "MEDIUM",
      "entryTime": "2026-05-15T10:30:00",
      "exitTime": "2026-05-15T12:45:00",
      "status": "PAID",
      "amount": 6.0
    }
  ]
}
```

---

## API Endpoints

### Parking Operations

| Method | Endpoint                          | Description                            | Body                                          |
|--------|-----------------------------------|----------------------------------------|-----------------------------------------------|
| POST   | `/api/parking/entry`              | Park a vehicle, get a ticket           | `{ "licensePlate": "X", "vehicleType": "CAR" }` |
| POST   | `/api/parking/exit/{ticketId}`    | Exit — calculates fee, frees spot       | —                                             |
| GET    | `/api/parking/ticket/{ticketId}`  | Get ticket details                     | —                                             |
| GET    | `/api/parking/status`             | Occupancy status (all floors)           | —                                             |
| GET    | `/api/parking/active-tickets`     | List all currently parked vehicles      | —                                             |
| GET    | `/api/parking/pricing`            | Current pricing rates                   | —                                             |

### Admin Operations

| Method | Endpoint                               | Description                | Body                                       |
|--------|----------------------------------------|----------------------------|--------------------------------------------|
| GET    | `/api/admin/parking-lot`               | Full raw data dump          | —                                          |
| POST   | `/api/admin/floors`                    | Add a new floor             | `{ "floorNumber": 4 }`                     |
| POST   | `/api/admin/floors/{floorId}/spots`    | Add a spot to a floor       | `{ "spotNumber": "F4-L1", "spotType": "LARGE" }` |

### Vehicle Types
`MOTORCYCLE` | `CAR` | `TRUCK`

### Spot Types
`SMALL` | `MEDIUM` | `LARGE`

### Pricing
| Vehicle    | Rate      | Billing       |
|------------|-----------|---------------|
| MOTORCYCLE | $1.00/hr  | Ceil to hour  |
| CAR        | $2.00/hr  | Ceil to hour  |
| TRUCK      | $3.50/hr  | Ceil to hour  |

Minimum charge: 1 hour regardless of actual duration.

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Start the Server

```bash
cd parking-lot
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.  
The JSON database is auto-created at `data/parking_lot.json` on first request.

### Build a runnable JAR

```bash
mvn clean package
java -jar target/parking-lot-1.0.0.jar
```

---

## Testing with cURL

### 1. Check parking lot status

```bash
curl -s http://localhost:8080/api/parking/status | python3 -m json.tool
```

### 2. See pricing rates

```bash
curl -s http://localhost:8080/api/parking/pricing | python3 -m json.tool
```

### 3. Park a motorcycle

```bash
curl -s -X POST http://localhost:8080/api/parking/entry \
  -H "Content-Type: application/json" \
  -d '{"licensePlate": "KA01EF5678", "vehicleType": "MOTORCYCLE"}' \
  | python3 -m json.tool
```

### 4. Park a car

```bash
curl -s -X POST http://localhost:8080/api/parking/entry \
  -H "Content-Type: application/json" \
  -d '{"licensePlate": "MH12AB1234", "vehicleType": "CAR"}' \
  | python3 -m json.tool
```

### 5. Park a truck

```bash
curl -s -X POST http://localhost:8080/api/parking/entry \
  -H "Content-Type: application/json" \
  -d '{"licensePlate": "DL03CD9999", "vehicleType": "TRUCK"}' \
  | python3 -m json.tool
```

### 6. Get ticket details (replace TKT-XXXXXXXX with real ticket id)

```bash
curl -s http://localhost:8080/api/parking/ticket/TKT-XXXXXXXX | python3 -m json.tool
```

### 7. Exit the parking lot

```bash
curl -s -X POST http://localhost:8080/api/parking/exit/TKT-XXXXXXXX \
  | python3 -m json.tool
```

Sample response:
```json
{
  "success": true,
  "message": "Payment successful. Safe drive!",
  "data": {
    "ticketId": "TKT-ABC12345",
    "licensePlate": "MH12AB1234",
    "vehicleType": "CAR",
    "floor": 1,
    "spot": "F1-M1",
    "entryTime": "2026-05-15T10:30:00",
    "exitTime": "2026-05-15T12:45:00",
    "durationMinutes": 135,
    "pricingRate": "$2.00/hour (min 1 hour)",
    "amountDue": "$6.00",
    "status": "PAID"
  }
}
```

### 8. See all active tickets

```bash
curl -s http://localhost:8080/api/parking/active-tickets | python3 -m json.tool
```

### 9. Add a new floor (admin)

```bash
curl -s -X POST http://localhost:8080/api/admin/floors \
  -H "Content-Type: application/json" \
  -d '{"floorNumber": 4}' \
  | python3 -m json.tool
```

### 10. Add a spot to a floor (admin — use real floorId from status response)

```bash
curl -s -X POST http://localhost:8080/api/admin/floors/{floorId}/spots \
  -H "Content-Type: application/json" \
  -d '{"spotNumber": "F4-L1", "spotType": "LARGE"}' \
  | python3 -m json.tool
```

### 11. Try to park a duplicate vehicle (expect 400)

```bash
curl -s -X POST http://localhost:8080/api/parking/entry \
  -H "Content-Type: application/json" \
  -d '{"licensePlate": "MH12AB1234", "vehicleType": "CAR"}' \
  | python3 -m json.tool
```

### 12. Full data dump (admin)

```bash
curl -s http://localhost:8080/api/admin/parking-lot | python3 -m json.tool
```

---

## Error Responses

All errors follow the same envelope:

```json
{
  "success": false,
  "message": "Vehicle MH12AB1234 is already parked.",
  "data": null
}
```

| HTTP Code | Scenario                                  |
|-----------|-------------------------------------------|
| 400       | Validation failure / business rule broken |
| 404       | Ticket not found                          |
| 409       | No parking spot available                 |
| 500       | Unexpected server error                   |
