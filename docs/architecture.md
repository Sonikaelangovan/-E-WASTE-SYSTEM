# Architecture and implementation status

The frontend is a Vite-built React single-page application. Axios calls a Spring Boot API. Spring Data JPA persists item submissions and append-only trace event rows in MySQL; Flyway owns schema creation. Docker Compose defines the local service topology. Jenkins and Ansible files are examples and have not been connected to external infrastructure.

The current model supports citizen accounts, administrator-managed account roles, owner-scoped submissions, collector/recycler assignment, scheduled pickups, role-checked lifecycle transitions recorded as trace events, persisted in-app notifications, and date/status-filtered reports. Lifecycle mutations lock the item row during validation and update. Reports scope citizen data to its owner and CSV exports omit pickup address, description, and account email. Facilities, inspections with evidence, recovered-material quantities, disposal certificates, and email/SMS delivery are not yet modeled.
