# Gestion_Services (Module) — 5ademni.tn

This repository contains the **`Gestion_Services`** module of the **5ademni.tn** application.  
It is a **Java (Maven)** module that integrates with the larger system and uses **MySQL** as its database.

> Path: `Gestion_Services/`  
> Build tool: **Maven**  
> Database: **MySQL**

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Run Locally](#run-locally)
- [Build](#build)
- [Tests](#tests)
- [Database](#database)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

`Gestion_Services` provides service-management functionality (domain/business logic and related APIs) as part of the **5ademni.tn** application.

Because it is a **module inside a bigger app**, it may depend on:
- shared libraries or parent `pom.xml` settings,
- common configuration conventions,
- other modules for authentication, users, etc.

---

## Tech Stack

- **Java** (main language)
- **Maven** (build & dependency management)
- **MySQL** (relational database)
- *(Optional / if present in the module)* Spring Boot / Spring MVC / Spring Data JPA

---

## Project Structure

Typical Maven layout:

```text
Gestion_Services/
├─ pom.xml
└─ src/
   ├─ main/
   │  ├─ java/
   │  └─ resources/
   └─ test/
      └─ java/
```

> If your structure differs, update this section to match the actual module layout.

---

## Prerequisites

- **JDK 17** (or the version required by the parent project)
- **Maven 3.8+**
- **MySQL 8+**
- Access to any required shared modules / parent project configuration

Check your Java version:
```bash
java -version
```

---

## Configuration

This module is expected to be configured via an `application.properties` or `application.yml` (often located in `src/main/resources/`) or via environment variables defined by the parent application.

### MySQL settings (example)

If you are using Spring Boot, typical properties look like:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/5ademni?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

> Replace values according to your environment and the conventions used in the main 5ademni.tn project.

---

## Run Locally

### 1) Create the MySQL database

```sql
CREATE DATABASE 5ademni;
```

### 2) Configure credentials

Update the module’s configuration (for example `src/main/resources/application.properties`) with your MySQL username/password and DB name.

### 3) Run with Maven

From the module directory:

```bash
cd Gestion_Services
mvn clean install
mvn spring-boot:run
```

If it is **not** a standalone Spring Boot module and must be run from the parent project, run the parent application instead (and keep this module included as a dependency).

---

## Build

```bash
mvn clean package
```

The built artifact will typically be located under:

```text
target/
```

---

## Tests

```bash
mvn test
```

---

## Database

- DBMS: **MySQL**
- Ensure MySQL is running locally or accessible remotely.
- If your project uses migrations (Flyway/Liquibase), run them as part of startup or build (depending on configuration).

---

## Troubleshooting

### Port already in use
If the module runs a web server (e.g., Spring Boot) and fails with “port already in use”, change the port:

```properties
server.port=808X
```

### Cannot connect to MySQL
- Confirm MySQL is running
- Validate host/port (`localhost:3306`)
- Verify username/password
- Ensure the database exists

---

## Contributing

1. Create a feature branch
2. Commit with clear messages
3. Open a Pull Request describing:
   - what changed,
   - how to test,
   - any configuration notes.

---

## License

Add the project license here (or reference the parent repository’s license).  
If no license is defined, default usage is **All rights reserved**.
