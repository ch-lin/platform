# Platform (Shared Library)

![Java](https://img.shields.io/badge/Java-25%2B-orange)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Architecture](https://img.shields.io/badge/Architecture-Shared%20Kernel-blueviolet)
![License](https://img.shields.io/badge/License-MIT-green)

**Platform** is the foundational shared library for the **YouTube Data Hub** ecosystem.

It serves as the "Shared Kernel" in our microservices architecture, housing common code, Data Transfer Objects (DTOs), and utility classes that are used across multiple services (Authentication, Downloader, and YouTube-Hub).

## 🎯 Purpose & Philosophy

In a distributed system, code duplication is a common pitfall. This project aims to:

1.  **DRY (Don't Repeat Yourself)**: Centralize logic that is repeated across services (e.g., Date formatting, JSON parsing utilities).
2.  **Contract Consistency**: Ensure that DTOs used for inter-service communication (e.g., the payload sent from Hub to Downloader) are identical on both ends.
3.  **Unified Dependencies**: Manage common dependency versions (like Lombok, HTTP Client) in one place to avoid version conflicts.

## 🛠️ Installation & Build (Crucial!)

**⚠️ Important**: Since the project is split into multiple repositories, **you must build and install this library locally first** before you can build `Authentication-Service`, `Downloader`, or `YouTube-Hub`.

### Prerequisites
*   Java 25+
*   Maven 3.9+

### Build Steps

Run the following command in the root of this directory to compile the code and install the JAR into your local Maven repository (`~/.m2/repository`).

```bash
mvn clean install
```

> If you skip this step, other services will fail to build with a "Could not find artifact ch.lin:platform" error.

## 💻 Usage in Other Services

To use this library in another microservice, add the following dependency to that service's `pom.xml`:

```xml
<dependency>
    <groupId>ch.lin</groupId>
    <artifactId>platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 🔄 Development Workflow

When you need to modify shared logic (e.g., adding a new field to a DTO):

1.  **Modify** the code in `Platform`.
2.  **Bump Version** (Optional, but recommended for production releases).
3.  **Run** `mvn clean install` in `Platform` to update your local registry.
4.  **Rebuild** the dependent services (e.g., `Downloader`) to pick up the changes.

## 📂 Project Structure

```text
Platform/
├── src/
│   ├── main/java/ch/lin/platform/
│   │   ├── dto/          # Shared Data Transfer Objects
│   │   ├── utils/        # Utility classes
│   │   └── ...
│   └── main/resources/
├── pom.xml               # Maven configuration
└── README.md
```

## 📜 License

MIT
