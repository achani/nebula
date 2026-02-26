# Confluent Schema Registry — Nebula Conventions

All inter-service asynchronous communication in Nebula uses **Apache Kafka**. To ensure type safety and backward compatibility across microservices, all messages must conform to **Avro schemas** stored in the Confluent Schema Registry.

## Conventions

1. **Naming:** Schema files should be placed in this folder and named `<Namespace>.<RecordName>.avsc`.
   - *Example:* `com.nebula.catalog.ProjectCreated.avsc`
2. **Evolution:** Schemas must be **BACKWARD COMPATIBLE**. You can add new optional fields (with defaults), but you cannot remove fields or change types.
3. **Registration:** During the CI pipeline (added in a later phase), the Gradle plugin will automatically register these `.avsc` files with the Schema Registry.
4. **Code Generation:** Spring Boot services use the `com.github.davidmc24.gradle.plugin.avro` Gradle plugin to generate Java classes from these schemas at compile time.

*This folder will be populated starting in Phase 2.*
