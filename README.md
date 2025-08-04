# Cloud-Native Email Ingestion Service

A Java service to ingest and analyze email archives (`.tar.gz`).

## API Endpoints

The service exposes the following REST API at `/`.

*   **`POST /start`**
    Accepts a `.tar.gz` archive via `multipart/form-data`. Responds with `202 Accepted` and begins processing asynchronously.

*   **`GET /status`**
    Returns a JSON object with the current ingestion status:
    *   `ingestionRunning`: `boolean` - True if a process is currently active.
    *   `messagesProcessed`: `int` - Total number of files attempted to be processed.
    *   `validSenderMessages`: `int` - Number of files from which a sender was successfully extracted.

*   **`GET /top-senders`**
    Returns a JSON array of the top 10 senders by message count.

## Key Design Decisions

*   **In-Memory Metrics:** Metrics are stored in-memory.
    *   **Rationale:** This is sufficient for the exercise's on-demand analysis use case, where results are only needed for a single run. This approach keeps the service self-contained with no external dependencies.
    *   **Extensibility:** Logic is decoupled via the `MetricsRepository` interface. A persistent store (e.g., Redis, Kafka) could be implemented, but would require a different interaction pattern (e.g., batching or event streaming) to be efficient at scale, as direct per-message network calls would be prohibitive.

*   **Asynchronous Processing:** Long-running ingestion is handled in a background thread pool (`@Async`) to keep the API responsive.

*   **Stream-Based File Handling:** The service streams the `.tar.gz` archive and processes it entry-by-entry to maintain a low memory footprint, regardless of archive size.

*   **Stateless Service:** The service itself is stateless, a core principle for horizontal scalability. State is managed by the backing repository (currently in-memory).

*   **Concurrency:** Shared state is managed with thread-safe constructs from `java.util.concurrent` to ensure data integrity.

*   **Containerization:** A multi-stage `Dockerfile` produces a minimal and secure runtime image using a Google Distroless base.

## Technology Stack

*   Java 21
*   Spring Boot 3.x
*   Apache Commons Compress
*   Jakarta Mail
*   Maven
*   Docker & Docker Compose
*   JUnit 5

## How to Run

1.  Ensure Docker Desktop is running.
2.  Navigate to the project root and execute:
    ```bash
    docker compose up --build
    ```
3.  The service will be available at `http://localhost:8080`.

## API Usage (cURL Examples)

**1. Start Ingestion**
```bash
# Replace [PATH_TO_YOUR_FILE] with the path to the enron .tar.gz file
curl -X POST -F "file=@[PATH_TO_YOUR_FILE]" http://localhost:8080/start
```

**2. Check Status**
```bash
curl http://localhost:8080/status
```
*Example Response (After Completion):*
```json
{
  "ingestionRunning": false,
  "messagesProcessed": 517401,
  "validSenderMessages": 493334
}
```

**3. Get Top Senders**
```bash
curl http://localhost:8080/top-senders
```
*Example Response:*
```json
[
  {
    "email": "kay.mann@enron.com",
    "count": 1634
  },
  {
    "email": "jeff.dasovich@enron.com",
    "count": 1599
  }
]
```