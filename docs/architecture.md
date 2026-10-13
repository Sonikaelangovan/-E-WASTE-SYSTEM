# Architecture and implementation status

The frontend is a Vite-built React single-page application. Axios calls a Spring Boot API. Spring Data JPA persists item submissions and append-only trace event rows in MySQL; Flyway owns schema creation. Docker Compose defines the local service topology. Jenkins and Ansible files are examples and have not been connected to external infrastructure.

The current model supports item submission and an initial trace event. It does not yet model users, roles, collectors, pickup schedules, facilities, inspections, recovered materials, or disposal. An end-to-end lifecycle must add those persisted models and enforce legal state transitions in a transactional service layer before the project can meet its full objective.
