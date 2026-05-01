# TaskFlow — Team Task Manager

A full-stack Team Task Management Web Application built with **Spring Boot**, **MySQL**, and vanilla **HTML/CSS/JS**.

---

## Features

- **Authentication**: JWT-based signup & login
- **Projects**: Create projects (creator becomes Admin), manage members
- **Tasks**: Full CRUD with title, description, due date, priority, status, assignee
- **Dashboard**: Real-time stats — total tasks, by status, by priority, overdue, per user
- **Role-Based Access**: Admin manages everything; Members update only their assigned tasks
- **Clean Dark UI**: Sidebar navigation, modals, charts, responsive tables

---

## Tech Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| Backend    | Spring Boot 3.2, Java 17          |
| Auth       | JWT (jjwt 0.11.5)                 |
| Database   | MySQL 8+                          |
| ORM        | Spring Data JPA / Hibernate       |
| Security   | Spring Security                   |
| Frontend   | HTML5, CSS3, Vanilla JS           |
| Build      | Maven                             |

---

## Database Schema

```
users          — id, name, email, password, role (ADMIN/MEMBER)
projects       — id, name, description, admin_id → users
project_members— project_id → projects, user_id → users  (many-to-many)
tasks          — id, title, description, due_date, priority, status, assigned_to_id → users, project_id → projects
```

---

## Local Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8+

### 1. Clone the repository
```bash
git clone <your-repo-url>
cd TaskManager
```

### 2. Create MySQL database
```sql
CREATE DATABASE taskdb;
```

### 3. Configure database credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 4. Build and run
```bash
./mvnw spring-boot:run
```
Or on Windows:
```bash
mvnw.cmd spring-boot:run
```

### 5. Open in browser
```
http://localhost:8080
```

The first registered user automatically becomes **Admin**.

---

## API Reference

All API endpoints (except auth) require `Authorization: Bearer <token>` header.

### Auth

| Method | Endpoint            | Body                                        | Description         |
|--------|---------------------|---------------------------------------------|---------------------|
| POST   | /api/auth/signup    | `{name, email, password}`                   | Register new user   |
| POST   | /api/auth/login     | `{email, password}`                         | Login, returns JWT  |
| GET    | /api/auth/me        | —                                           | Get current user    |

### Projects

| Method | Endpoint                              | Role   | Description              |
|--------|---------------------------------------|--------|--------------------------|
| POST   | /api/projects                         | Any    | Create project (becomes admin) |
| GET    | /api/projects                         | Any    | List projects            |
| GET    | /api/projects/{id}                    | Any    | Get project              |
| PUT    | /api/projects/{id}                    | Admin  | Update project           |
| DELETE | /api/projects/{id}                    | Admin  | Delete project           |
| POST   | /api/projects/{id}/members/{userId}   | Admin  | Add member               |
| DELETE | /api/projects/{id}/members/{userId}   | Admin  | Remove member            |
| GET    | /api/projects/{id}/members            | Any    | List members             |

### Tasks

| Method | Endpoint                    | Role          | Description               |
|--------|-----------------------------|---------------|---------------------------|
| POST   | /api/tasks                  | Admin         | Create task               |
| GET    | /api/tasks                  | Any           | List tasks (filtered by role) |
| GET    | /api/tasks/{id}             | Any           | Get task                  |
| GET    | /api/tasks/project/{id}     | Any           | Tasks by project          |
| PUT    | /api/tasks/{id}             | Admin/Assignee| Update task               |
| PATCH  | /api/tasks/{id}/status      | Admin/Assignee| Update status only        |
| DELETE | /api/tasks/{id}             | Admin         | Delete task               |

### Dashboard

| Method | Endpoint       | Description                              |
|--------|----------------|------------------------------------------|
| GET    | /api/dashboard | Stats: totals, by status, overdue, per user |

### Users (Admin only)

| Method | Endpoint              | Description       |
|--------|-----------------------|-------------------|
| GET    | /api/users            | List all users    |
| GET    | /api/users/{id}       | Get user          |
| PATCH  | /api/users/{id}/role  | Update role       |
| DELETE | /api/users/{id}       | Delete user       |

---

## Postman Testing

### 1. Signup
```
POST http://localhost:8080/api/auth/signup
Content-Type: application/json

{
  "name": "John Admin",
  "email": "admin@test.com",
  "password": "password123"
}
```

### 2. Login & get token
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@test.com",
  "password": "password123"
}
```
Copy the `token` from the response.

### 3. Create project (set Authorization: Bearer <token>)
```
POST http://localhost:8080/api/projects
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "My First Project",
  "description": "Testing the API"
}
```

### 4. Create task
```
POST http://localhost:8080/api/tasks
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Design homepage",
  "description": "Create wireframes",
  "priority": "HIGH",
  "status": "TODO",
  "dueDate": "2025-12-31",
  "projectId": 1,
  "assignedToId": 1
}
```

### 5. Dashboard
```
GET http://localhost:8080/api/dashboard
Authorization: Bearer <token>
```

---

## Deployment on Railway

### 1. Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin <your-repo>
git push -u origin main
```

### 2. Create Railway project
1. Go to [railway.app](https://railway.app) → New Project
2. Add **MySQL** service (Railway provides it)
3. Add **GitHub Repo** service pointing to your repo

### 3. Set Environment Variables on Railway
```
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQL_HOST}}:${{MySQL.MYSQL_PORT}}/${{MySQL.MYSQL_DATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQL_USER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQL_PASSWORD}}
JWT_SECRET=bXlfc3VwZXJfc2VjdXJlX2p3dF9zZWNyZXRfa2V5X2Zvcl90ZWFtdGFza19hcHBfMjAyNA==
JWT_EXPIRATION=86400000
```

### 4. Deploy
Railway auto-deploys on push. Your app will be live at `https://<your-app>.up.railway.app`.

---

## Roles

| Action                   | Admin | Member |
|--------------------------|-------|--------|
| Create project           | ✅    | ✅     |
| Manage project members   | ✅    | ❌     |
| Delete project           | ✅    | ❌     |
| Create task              | ✅    | ❌     |
| Edit task (all fields)   | ✅    | ❌     |
| Update own task status   | ✅    | ✅     |
| View all tasks           | ✅    | ❌     |
| View assigned tasks      | ✅    | ✅     |
| Manage users             | ✅    | ❌     |
| View full dashboard      | ✅    | ✅*    |

*Members see stats for their own assigned tasks only.
