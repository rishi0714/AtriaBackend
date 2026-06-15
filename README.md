<div align="center">

<h1>🎓 ATRIA</h1>

**Campus Engagement & Event Management Platform**

Connect students, clubs, and college administrators through a unified ecosystem for events, registrations, attendance, and analytics.

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![React](https://img.shields.io/badge/React-Vite-61DAFB?style=flat-square&logo=react&logoColor=black)

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
| Frontend | React + Vite + JavaScript |
| Build | Maven |

---

## 🗃️ Database Schema (Entities)

| Entity | Description |
|--------|-------------|
| `User` | User accounts with roles and profile details |
| `College` | College records with domain management |
| `Club` | Club records linked to colleges |
| `Event` | Events created by clubs with lifecycle tracking |
| `Registration` | Student registrations per event |
| `Attendance` | QR check-in records per registration |
| `DailyProgress` | Daily snapshot of event and registration metrics |
| `RefreshToken` | Hashed refresh tokens for secure session management |

> ⚠️ ATRIA uses `ddl-auto: validate` — Hibernate does **not** auto-create tables. The database schema must be set up manually before starting the application.

---

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| PostgreSQL | 14+ |
| Node.js | 18+ |
| npm | 9+ |

You will also need:
- A **Google OAuth2** credentials (Client ID & Secret) from [Google Cloud Console](https://console.cloud.google.com/)
- A **Cloudinary** account for media storage
- A **Gmail** account with App Password enabled for mail notifications

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/rishi0714/Atria.git
cd Atria
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

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at: `http://localhost:5173`

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

## 📡 API Overview

| Group | Base Route | Description |
|-------|-----------|-------------|
| Authentication | `/api/auth` | Login, token refresh, logout |
| Current User | `/api/me` | Profile, profile completion |
| Platform Owner | `/api/platform` | College management, system analytics |
| Colleges | `/api/platform/colleges` | Create colleges, manage domains, assign admins |
| Clubs | `/api/club` | Club dashboard, analytics, event & member management |
| Events | `/api/events` | Event creation, updates, discovery |
| Registrations | `/api/registrations` | Register, cancel, track registration status |
| Attendance | `/api/attendance` | QR check-ins, attendance tracking |
| Users | `/api/users` | User management, role assignment |

---

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

<div align="center">
  <p>Built with ❤️ by <a href="https://github.com/rishi0714">Rishi Kumar Uppalapati</a></p>
</div>
