<div align="center">

<h1>🎓 ATRIA</h1>

**Campus Engagement & Event Management Platform**

Connect students, clubs, and college administrators through a unified ecosystem for events, registrations, attendance, and analytics.

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

</div>

---

## 💡 What is ATRIA?

ATRIA is a **full-stack campus engagement platform** built for colleges, clubs, and students who want to **organize events at scale**, **track attendance accurately**, and **make data-driven decisions** through analytics.

The backend is designed as a **production-ready Java application** with a clean multi-tenant architecture — supporting multiple colleges, each with their own admins, clubs, and events under a single platform.

### 🎯 Key Problems It Solves

- **No centralized event management** → Full event lifecycle from creation to attendance tracking
- **Manual attendance processes** → QR-based check-in with real-time monitoring
- **Scattered club operations** → Unified club dashboards with member and event management
- **Lack of campus insights** → Analytics dashboards with year-wise & stream-wise distributions
- **Complex multi-role administration** → Clean RBAC across platform, college, club, and student levels

---

## ✨ Features

### 🔐 Authentication & Security
- Google OAuth2 login with JWT access & refresh token management
- Role-Based Access Control (RBAC) — Platform Owner, College Admin, Club Admin, Student, Guest
- Secure API authorization via Spring Security

### 🏛️ Multi-Level Administration
- **Platform Owner** — manages all colleges, assigns college admins, views system-wide analytics
- **College Admin** — manages clubs, college domains, college-level analytics
- **Club Admin** — manages club members, events, and club performance
- **Student / Guest** — discovers events, registers, and tracks attendance

### 📅 Event Management
- Event creation, publishing, and updates
- Registration workflows with participant management
- Event categorization, discovery, and event-specific insights

### ✅ Attendance Tracking
- QR-based attendance system
- Real-time check-in monitoring
- Registration-to-attendance conversion tracking
- Attendance analytics per event

### 📊 Analytics & Insights
- Registration and attendance statistics
- Year-wise and stream-wise participant distribution
- Club and event performance tracking
- Actionable dashboards for all admin levels

### ☁️ Media Management
- Cloudinary integration for event banner uploads
- Secure cloud-based asset storage

---

## 🏗️ Architecture Overview

```
Platform Owner
└── manages Colleges
      └── College Admins
            └── manage Clubs
                  └── Club Admins
                        └── manage Events
                              └── Students / Guests
                                    ├── Register
                                    ├── Attend (QR Check-in)
                                    └── View Analytics
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17+ |
| Framework | Spring Boot 3.2.x |
| Security | Spring Security + JWT + Google OAuth2 |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 14+ |
| Media Storage | Cloudinary |
| Email | JavaMail (Gmail) |
| Containerization | Docker |
| Build | Maven |

---

## 🗃️ Database Schema (Entities)

| Entity | Key Fields | Description |
|--------|-----------|-------------|
| `users` | `user_id`, `email`, `full_name`, `role`, `college_id`, `stream`, `year`, `registration_number` | User accounts with roles, profile details, and college association |
| `colleges` | `college_id`, `name`, `logo_url`, `is_active` | College records managed by the platform owner |
| `college_domains` | `id`, `college_id`, `domain`, `is_primary` | Email domains linked to colleges for student verification |
| `clubs` | `club_id`, `name`, `college_id`, `managed_by`, `club_category`, `description`, `logo_url`, `is_active` | Clubs linked to colleges with category and admin assignment |
| `events` | `event_id`, `club_id`, `college_id`, `title`, `status`, `event_date`, `max_capacity`, `registration_deadline`, `poster_url`, `rejection_reason` | Full event lifecycle including status, capacity, and scheduling |
| `registrations` | `registration_id`, `event_id`, `user_id`, `qr_code`, `is_cancelled`, `registered_at` | Student registrations per event with QR code generation |
| `attendance` | `attendance_id`, `registration_id`, `scanned_by`, `scanned_at` | QR-based check-in records linked to registrations |

> ⚠️ Flyway is currently disabled and `ddl-auto` is set to `validate` — Hibernate will **not** create tables automatically. The database schema must be created manually before starting the application.

---

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| PostgreSQL | 14+ |

You will also need:
- A **Google OAuth2** credentials (Client ID & Secret) from [Google Cloud Console](https://console.cloud.google.com/)
- A **Cloudinary** account for media storage
- A **Gmail** account with App Password enabled for mail notifications

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/rishi0714/AtriaBackend.git
cd AtriaBackend
```

### 2. Set up PostgreSQL database

```sql
CREATE DATABASE campus_platform;
```

### 3. Configure environment variables

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/campus_platform
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# JWT (RSA Keys)
JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----

# Mail
MAIL_USERNAME=yourmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password

# Platform Owner (seeded on first run)
PLATFORM_OWNER_EMAIL=owner@example.com
PLATFORM_OWNER_NAME=Platform Owner

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

### 4. Build and run the backend

```bash
mvn clean install
mvn spring-boot:run
```

Backend runs at: `http://localhost:8081`

---

## 🔑 Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_URL` | PostgreSQL JDBC URL | ✅ |
| `DB_USERNAME` | Database username | ✅ |
| `DB_PASSWORD` | Database password | ✅ |
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | ✅ |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | ✅ |
| `JWT_PRIVATE_KEY` | RSA private key for signing JWTs | ✅ |
| `JWT_PUBLIC_KEY` | RSA public key for verifying JWTs | ✅ |
| `MAIL_USERNAME` | Gmail account for notifications | ✅ |
| `MAIL_PASSWORD` | Gmail App Password | ✅ |
| `PLATFORM_OWNER_EMAIL` | Initial platform owner email | ✅ |
| `PLATFORM_OWNER_NAME` | Initial platform owner name | ✅ |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | ✅ |
| `CLOUDINARY_API_KEY` | Cloudinary API key | ✅ |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | ✅ |

---

## 📖 API Documentation

Once the backend is running, explore the full API via:

| | URL |
|--|-----|
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| OpenAPI Spec | `http://localhost:8081/v3/api-docs` |

---

## 📁 Project Structure

```
src/main/java/com/campus/platform/
├── CampusPlatformApplication.java       # Entrypoint
│
├── security/                            # Security infrastructure
│   ├── auth/                            # Auth controller, service, DTOs
│   ├── config/                          # SecurityConfig, CloudinaryConfig, OpenApiConfig
│   ├── jwt/                             # JWT filter, token provider, principal
│   └── oauth2/                          # Google OAuth2 handlers & services
│
├── user/                                # User management
│   ├── controller/                      # UserController, StudentController
│   ├── entity/                          # User.java
│   ├── service/                         # UserService
│   ├── mapper/ & repository/ & dto/
│
├── college/                             # College & domain management
│   ├── controller/                      # CollegeController
│   ├── entity/                          # College.java, CollegeDomain.java
│   ├── service/                         # CollegeService
│   ├── mapper/ & repository/ & dto/
│
├── club/                                # Club management & analytics
│   ├── controller/                      # ClubController
│   ├── entity/                          # Club.java
│   ├── service/                         # ClubService, AnalyticsService
│   ├── mapper/ & repository/ & dto/
│
├── event/                               # Event lifecycle
│   ├── controller/                      # EventController
│   ├── entity/                          # Event.java
│   ├── scheduler/                       # EventScheduler
│   ├── service/                         # EventService
│   ├── mapper/ & repository/ & dto/
│
├── registration/                        # Event registrations
│   ├── controller/                      # RegistrationController
│   ├── entity/                          # Registration.java
│   ├── service/                         # RegistrationService
│   ├── mapper/ & repository/ & dto/
│
├── attendance/                          # QR-based attendance
│   ├── controller/                      # AttendanceController
│   ├── entity/                          # Attendance.java
│   ├── service/                         # AttendanceService
│   ├── mapper/ & repository/ & dto/
│
├── dashboard/                           # Role-specific dashboards
│   ├── controller/                      # DashboardController
│   ├── service/                         # DashboardService
│   └── dto/                             # SuperAdmin, CollegeAdmin, ClubAdmin, Student DTOs
│
├── upload/                              # Cloudinary media uploads
│   ├── controller/                      # UploadController
│   └── service/                         # UploadService
│
├── export/                              # CSV data export
│   ├── CsvExportController.java
│   └── CsvExportService.java
│
├── bootstrap/                           # Seed data on startup
│   └── PlatformOwnerBootstrap.java      # Creates initial platform owner
│
├── common/                              # Shared utilities
│   ├── enums/                           # EventStatus, UserRole
│   ├── response/                        # ApiResponse wrapper
│   ├── service/                         # EmailService, QrCodeService
│   └── util/                            # SecurityContextUtil
│
└── exception/                           # Global exception handling
    ├── GlobalExceptionHandler.java
    ├── DuplicateResourceException.java
    ├── ResourceNotFoundException.java
    └── TenantAccessDeniedException.java
```

---

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## 🧑‍💻 Author

<div align="center">

**Rishi Kumar Uppalapati**

[![GitHub](https://img.shields.io/badge/GitHub-rishi0714-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/rishi0714)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/rishi-kumar-uppalapati-02333928a)

⭐ If this project helped or inspired you, give it a star!

</div>
