# Cloud-Native Email Ingestion Service

A self-contained, "cloud-native" Java service to ingest and provide metrics for email archives, built as a solution to a coding exercise.

## Features

The service provides the following HTTP endpoints as required by the prompt:

*   **`POST /start`**: Accepts a `.tar.gz` archive and begins processing asynchronously.
*   **`GET /status`**: Returns the current ingestion status and the total number of files processed.
*   **`GET /top-senders`**: Returns the top 10 email senders by message count.

## Design Philosophy & Justification

The architecture of this service is a direct and deliberate response to the requirements of the coding exercise. Key trade-offs were made to deliver a system that is simple, self-contained, and runnable locally, while still being designed for a cloud environment. Explanation of truly "cloud-native" at the end of README

1.  **Request-Driven Ingestion (`POST /start`)**: The prompt explicitly asks for a `POST /start` endpoint, implying a request-driven model where a client actively initiates the process. My design implements this contract precisely, streaming the uploaded file to ensure a low memory footprint.

2.  **In-Memory State**: To keep the application simple and self-contained, the ingestion status and sender counts are managed in-memory. This is an acceptable trade-off for the on-demand analysis use case described, but its limitations are recognized.
    *   **Extensibility:** The state management is abstracted behind a `MetricsRepository` interface, which is the key enabler for the more advanced architecture discussed below.

## Excersize Technology Stack

*   Java 21 & Spring Boot 3.x
*   Apache Commons Compress & Jakarta Mail
*   Docker & Docker Compose for local execution
*   Maven & JUnit 5

## How to Run

1.  Ensure Docker Desktop is running.
2.  From the project root, run:
    ```bash
    docker-compose up --build
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
*Example Response (using your 90-message test data):*
```json
{
  "ingestionRunning": false,
  "messagesProcessed": 90
}
```

**3. Get Top Senders**
```bash
curl http://localhost:8080/top-senders```
*Example Response (using your 90-message test data):*
```json
[
  {
    "email": "sender1@example.com",
    "count": 13
  },
  {
    "email": "sender2@example.com",
    "count": 12
  }
]
```

### "Cloud-Native" Architecture

The true "cloud-native" solution separates the responsibilities of ingestion, storage, and analytics.

*   **The "Write Path" (High-Throughput Data Ingestion):**
    The process would no longer be a single, long-running job. The service's responsibility would change fundamentally:
    1.  A **Cloud Run** job is triggered by a file upload to **GCS**, receiving an event from **Pub/Sub**.
    2.  Its *only* job is to parse the file and stream the raw, un-aggregated data—one record per valid email—into a high-throughput ingestion service. The right tool for this is **Google BigQuery's Streaming API**. This is not a database update; it's a fire-and-forget append operation designed for massive scale. Redis would not be used here, as it is not an optimal tool for ingesting raw event streams.

*   **The "Read Path" (On-Demand Analytics):**
    With raw data flowing into BigQuery, the `GET /top-senders` endpoint would be part of a **separate "Metrics API" service**. Its job is to execute a simple SQL query against for e.g the BigQuery table on demand:

    This leverages BigQuery's power to perform analytics across terabytes of data in seconds, completely decoupling the query logic from the ingestion process.

*   **The "Monitoring Path" (Job Status):**
    The concept of a single `GET /status` for one big job becomes obsolete. In a "fleet of jobs" model, you monitor the health of the **pipeline itself**:
    1.  **Ingestion Volume:** You monitor the number of incoming messages on the **Pub/Sub** topic.
    2.  **Processing Health:** You use **Cloud Logging** and **Cloud Monitoring** to create log-based metrics. For example, you would create charts showing the rate of "Successfully Parsed" logs versus "Malformed Data" warnings from the Cloud Run jobs.
    3.  **Data Freshness:** A dashboard would monitor the timestamp of the latest record inserted into BigQuery.