# Cloud-Native Email Ingestion Service

A self-contained, "cloud-native" Java service to ingest and provide metrics for email archives, built as a solution to a coding exercise.

## Features

The service provides the following HTTP endpoints as required by the prompt:

*   **`POST /start`**: Accepts a `.tar.gz` archive and begins processing asynchronously.
*   **`GET /status`**: Returns the current ingestion status and the total number of files processed.
*   **`GET /top-senders`**: Returns the top 10 email senders by message count.

## Design

Matches requirements of the coding exercise. Trade-offs were made to deliver a system that is simple, self-contained, and runnable locally, while still being designed for a cloud environment. 

1.  **Request-Driven Ingestion (`POST /start`)**: The prompt explicitly asks for a `POST /start` endpoint, implying a request-driven model where a client actively initiates the process. The archive is sent to the end point and streamed for a low memory footprint.

2.  **In-Memory State**: I used an in-memory `ConcurrentHashMap` to store sender metrics during ingestion. This allowed the service to run without requiring external systems like Redis or a database.  
(This approach does introduce a bottleneck for very large datasets and wouldn't scale well if the ingestion needed to run frequently or across multiple instances.)

If durability or distributed processing were required, the same interface could support a Redis-backed or database-backed implementation. The current setup keeps things minimal and avoids introducing complexity that may not be necessary for a one-off, single-node ingestion task.

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
4. Dataset available at https://www.cs.cmu.edu/~enron/enron_mail_20150507.tar.gz

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

Example Response (using your 90-message test data):

```json
{
  "ingestionRunning": false,
  "messagesProcessed": 90,
  "validSenderMessages": 90,
}
```

**3. Get Top Senders**
```bash
curl http://localhost:8080/top-senders
```

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

## "Cloud-Native" version of architecture 

A more cloud-native approach would separate ingestion, storage, and analytics into independent responsibilities:

**High-Throughput Data Ingestion**

Rather than a single long-running job, ingestion would be event-driven and distributed. For example:

1. A file upload to **Cloud Storage** triggers a **Cloud Run** job via **Pub/Sub**.
2. That job parses the archive and streams raw, unaggregated records—one per valid email—into a high-throughput sink such as **BigQuery's Streaming API**.

**On-Demand Analytics**

Metrics endpoints like `GET /top-senders` would be served by a **separate service**, querying the raw data store (e.g. BigQuery) on demand. This decouples analytics from ingestion entirely, allowing for stateless, scalable query services.

**Job Status**

In this model, `GET /status` as a single-process snapshot is replaced by pipeline-level observability:

- **Ingestion Volume**: Monitored via **Pub/Sub** topic metrics.
- **Processing Health**: Derived from **log-based metrics** in **Cloud Logging**, e.g. "Successfully Parsed" vs. "Malformed Data".
- **Data Freshness**: Gauged from the timestamp of the latest record inserted into BigQuery.

