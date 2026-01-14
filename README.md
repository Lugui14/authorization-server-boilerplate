# Authorization Server Boilerplate

A multi-module Spring Boot OAuth2 Authorization Server with JWT token support, OAuth2 social login (Google, GitHub), and a sample Resource Server API.

## 🏗️ Architecture

This project consists of three modules:

- **common**: Shared DTOs and utilities
- **authorization-server**: OAuth2 Authorization Server issuing JWT tokens (port 8080)
- **resource-server**: Protected REST API validating JWT tokens (port 8081)

## 🚀 Features

- ✅ OAuth2 Authorization Server with Spring Security
- ✅ JWT tokens signed with RSA keys
- ✅ Username/password authentication
- ✅ OAuth2 social login (Google & GitHub)
- ✅ User persistence in PostgreSQL
- ✅ Resource Server with JWT validation
- ✅ Role-based access control (RBAC)
- ✅ Docker Compose setup with PostgreSQL
- ✅ Ready for future ABAC/RBAC extensions
- ✅ Flyway database migrations

## 📋 Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- keytool (included with JDK)

## 🔧 Setup

### 1. Generate RSA Keystore

Generate the RSA key pair for JWT signing:

```bash
chmod +x generate-keystore.sh
./generate-keystore.sh
```

This creates a keystore at `authorization-server/src/main/resources/jwt-keystore.jks`.

### 2. Configure OAuth2 Providers

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and add your OAuth2 credentials:

#### Google OAuth2 Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable "Google+ API"
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
5. Application type: "Web application"
6. Authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google`
7. Copy Client ID and Client Secret to `.env`

#### GitHub OAuth2 Setup
1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in:
   - Application name: Your app name
   - Homepage URL: `http://localhost:8080`
   - Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. Copy Client ID and Client Secret to `.env`

### 3. Start PostgreSQL

```bash
docker-compose up -d postgres
```

Verify PostgreSQL is running:
```bash
docker-compose ps
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Servers

**Option A: Run both servers locally**

Terminal 1 - Authorization Server:
```bash
cd authorization-server
mvn spring-boot:run
```

Terminal 2 - Resource Server:
```bash
cd resource-server
mvn spring-boot:run
```

**Option B: Run in Docker (after uncommenting services in docker-compose.yml)**

```bash
docker-compose up -d
```

## 🔐 Default Users

The system comes with two pre-configured users:

| Email | Password | Roles |
|-------|----------|-------|
| admin@example.com | admin123 | ADMIN, USER |
| user@example.com | user123 | USER |

## 🧪 Testing the Flow

### 1. Get Access Token (Username/Password)

**Using Authorization Code Flow with PKCE (Recommended for public clients):**

1. Open browser and navigate to:
```
http://localhost:8080/oauth2/authorize?response_type=code&client_id=public-client&redirect_uri=http://localhost:3000/callback&scope=openid%20profile%20email%20read%20write&code_challenge=CHALLENGE&code_challenge_method=S256
```

2. Login with credentials (e.g., `user@example.com` / `user123`)
3. You'll be redirected with an authorization code
4. Exchange code for token (implement PKCE flow in your client)

**Using Confidential Client (for Insomnia/Postman):**

Configure OAuth2 in Insomnia/Postman:
- Grant Type: Authorization Code
- Authorization URL: `http://localhost:8080/oauth2/authorize`
- Access Token URL: `http://localhost:8080/oauth2/token`
- Client ID: `confidential-client`
- Client Secret: `secret`
- Redirect URL: `https://oauth.pstmn.io/v1/callback`
- Scope: `openid profile email read write`

### 2. Test OAuth2 Social Login

**Google Login:**
```
http://localhost:8080/oauth2/authorization/google
```

**GitHub Login:**
```
http://localhost:8080/oauth2/authorization/github
```

### 3. Access Resource Server Endpoints

**Public Endpoint (no authentication):**
```bash
curl http://localhost:8081/api/public/hello
```

**User Profile (requires authentication):**
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
     http://localhost:8081/api/user/profile
```

**User Data (requires USER role):**
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
     http://localhost:8081/api/user/data
```

**Admin Dashboard (requires ADMIN role):**
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
     http://localhost:8081/api/admin/dashboard
```

## 📚 API Endpoints

### Authorization Server (port 8080)

| Endpoint | Description |
|----------|-------------|
| `GET /oauth2/authorize` | OAuth2 authorization endpoint |
| `POST /oauth2/token` | Token endpoint |
| `GET /oauth2/jwks` | JWK Set endpoint (public keys) |
| `GET /login` | Login page |
| `GET /oauth2/authorization/google` | Google OAuth2 login |
| `GET /oauth2/authorization/github` | GitHub OAuth2 login |
| `GET /.well-known/openid-configuration` | OpenID Discovery |

### Resource Server (port 8081)

| Endpoint | Auth Required | Role Required |
|----------|---------------|---------------|
| `GET /api/public/hello` | No | - |
| `GET /api/user/profile` | Yes | USER |
| `GET /api/user/data` | Yes | USER |
| `GET /api/admin/dashboard` | Yes | ADMIN |
| `GET /api/health` | No | - |

## 🗄️ Database Schema

### Users Table
```sql
- id: BIGINT (PK)
- email: VARCHAR(255) UNIQUE
- password: VARCHAR(255)
- name: VARCHAR(255)
- enabled: BOOLEAN
- account_non_expired: BOOLEAN
- account_non_locked: BOOLEAN
- credentials_non_expired: BOOLEAN
- provider: VARCHAR(50) [LOCAL, GOOGLE, GITHUB]
- provider_id: VARCHAR(255)
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### User Roles Table
```sql
- user_id: BIGINT (FK)
- role: VARCHAR(50)
```

## 🔄 OAuth2 Grant Types Supported

- ✅ Authorization Code with PKCE
- ✅ Authorization Code (confidential clients)
- ✅ Refresh Token
- ✅ Client Credentials

## 🎯 Roadmap

This is the foundation for implementing:

- [ ] Attribute-Based Access Control (ABAC)
- [ ] Advanced Role-Based Access Control (RBAC)
- [ ] Configuration file for feature toggling
- [ ] Microservices authorization
- [ ] Token introspection
- [ ] Token revocation
- [ ] User management API
- [ ] Permission management
- [ ] Dynamic client registration

## 🛠️ Development

### Clean and rebuild
```bash
mvn clean install
```

### Run tests
```bash
mvn test
```

### View logs
```bash
# Authorization Server
tail -f authorization-server/logs/application.log

# Resource Server
tail -f resource-server/logs/application.log

# Docker logs
docker-compose logs -f
```

### Connect to PostgreSQL
```bash
docker exec -it auth-postgres psql -U authuser -d authdb
```

## 👤 Author

Luiz Guilherme

---

**Note**: This is a development setup. For production:
- Use proper secret management (Vault, AWS Secrets Manager)
- Enable HTTPS with valid certificates
- Use production-grade database configuration
- Implement rate limiting
- Add monitoring and logging
- Review security configurations

