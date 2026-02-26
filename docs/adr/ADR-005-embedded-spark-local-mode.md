# ADR 005: Embedded Spark Local Mode for Dataset Service

## Status
Accepted

## Context
In Phase 4, we are building the Dataset Service, which needs to manage Delta Lake tables on MinIO. Specifically, it needs to handle dataset creation (DDL), schema retrieval, metadata tracking, and time-travel reads of Delta table history.

While there are alternative JVM libraries for Delta Lake (e.g., Delta Standalone, which is read-centric, or the newer Delta Kernel), Apache Spark remains the most robust, canonical, and feature-complete way to interact with Delta Lake tables. However, spinning up a remote Spark cluster or maintaining a Thrift server just to execute fast lightweight DDL and metadata operations introduces significant operational overhead and latency.

## Decision
We will embed Apache Spark (version 3.5.x) running in `local[*]` mode directly inside the `dataset-service` Spring Boot application. 

This embedded `SparkSession` will be strictly reserved for **metadata and DDL operations only** (e.g., creating tables, reading table history, evolving schemas). Data processing, heavy transformations, and ETL jobs will NOT be executed by the Dataset Service; those workloads will be delegated to the Build Service (Phase 6) which will run on a dedicated Spark cluster.

## Consequences

### Positive
- **Native Delta Lake Capabilities**: We gain full access to the official Delta Lake APIs, ensuring compatibility and ease of development.
- **Architectural Simplicity**: No need for a complex remote Spark job submission mechanism just to create a table or query its schema.
- **Low Latency**: Metadata operations bypass cluster scheduling overhead.

### Negative / Mitigation
- **Memory Footprint**: Spark is memory-intensive. We must aggressively tune the embedded `SparkSession` (e.g., `spark.driver.memory`, `spark.sql.shuffle.partitions=1`) to keep the Spring Boot application lightweight.
- **Dependency Conflicts**: Spark brings a massive dependency tree (Hadoop, Netty, Jackson). We will need to carefully manage dependency exclusions in `build.gradle` to avoid conflicts with Spring Boot 3.
- **Classloader Issues**: Spark's classloading can sometimes clash with Spring Boot's executable JAR structure. We will rely on standard Gradle application plugins and carefully construct the fat JAR or run in exploded mode in Docker.
