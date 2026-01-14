# Configuration Guide

## Application Properties Reference

### JWT Configuration (`app.jwt`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `app.jwt.issuer` | String | `http://localhost:8080` | The issuer URL for JWT tokens |
| `app.jwt.access-token-expiration-seconds` | Long | `3600` | Access token expiration time in seconds (1 hour) |
| `app.jwt.refresh-token-expiration-seconds` | Long | `604800` | Refresh token expiration time in seconds (7 days) |

### CORS Configuration (`app.cors`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `app.cors.allowed-origins` | List<String> | See below | List of allowed origins for CORS |

**Default allowed origins:**
- `http://localhost:3000` - Frontend application
- `http://localhost:8081` - Resource server

### OAuth2 Configuration (`app.oauth2`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `app.oauth2.authorized-redirect-uris` | List<String> | See below | List of authorized redirect URIs after OAuth2 authentication |

**Default redirect URIs:**
- `http://localhost:8081/login/oauth2/code/client`
- `http://localhost:3000/oauth2/redirect`

## Environment-Specific Configuration

### Development (application-dev.yml)

```yaml
app:
  jwt:
    issuer: http://localhost:8080
    access-token-expiration-seconds: 3600
    refresh-token-expiration-seconds: 604800
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8081
      - http://localhost:4200
  oauth2:
    authorized-redirect-uris:
      - http://localhost:8081/login/oauth2/code/client
      - http://localhost:3000/oauth2/redirect
```

### Production (application-prod.yml)

```yaml
app:
  jwt:
    issuer: https://auth.yourdomain.com
    access-token-expiration-seconds: 1800      # 30 minutes for better security
    refresh-token-expiration-seconds: 86400    # 1 day
  cors:
    allowed-origins:
      - https://app.yourdomain.com
      - https://api.yourdomain.com
  oauth2:
    authorized-redirect-uris:
      - https://app.yourdomain.com/oauth2/redirect
      - https://api.yourdomain.com/login/oauth2/code/client
```

## Using Environment Variables

You can override any configuration using environment variables:

```bash
# JWT Configuration
export APP_JWT_ISSUER=https://auth.production.com
export APP_JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=1800
export APP_JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=86400

# CORS Configuration
export APP_CORS_ALLOWED_ORIGINS[0]=https://app.production.com
export APP_CORS_ALLOWED_ORIGINS[1]=https://api.production.com

# OAuth2 Configuration
export APP_OAUTH2_AUTHORIZED_REDIRECT_URIS[0]=https://app.production.com/callback
```

## Docker Deployment

### Using environment variables in docker-compose.yml

```yaml
services:
  authorization-server:
    image: authorization-server:latest
    environment:
      - APP_JWT_ISSUER=https://auth.production.com
      - APP_JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=1800
      - APP_CORS_ALLOWED_ORIGINS[0]=https://app.production.com
      - APP_CORS_ALLOWED_ORIGINS[1]=https://api.production.com
```

### Using external configuration file

```yaml
services:
  authorization-server:
    image: authorization-server:latest
    volumes:
      - ./config/application-prod.yml:/app/config/application-prod.yml
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_CONFIG_LOCATION=/app/config/
```

## Command Line Arguments

```bash
java -jar authorization-server.jar \
  --app.jwt.issuer=https://auth.example.com \
  --app.jwt.access-token-expiration-seconds=1800 \
  --app.cors.allowed-origins[0]=https://app.example.com
```

## Security Best Practices

### Production Settings

1. **Use HTTPS for issuer URL**
   ```yaml
   app:
     jwt:
       issuer: https://auth.yourdomain.com  # Always use HTTPS in production
   ```

2. **Shorter token expiration times**
   ```yaml
   app:
     jwt:
       access-token-expiration-seconds: 900    # 15 minutes
       refresh-token-expiration-seconds: 3600  # 1 hour
   ```

3. **Restrict CORS origins**
   ```yaml
   app:
     cors:
       allowed-origins:
         - https://app.yourdomain.com  # Only allow specific domains
   ```

4. **Use environment variables for secrets**
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: ${GOOGLE_CLIENT_ID}
               client-secret: ${GOOGLE_CLIENT_SECRET}
   ```

## Registering OAuth2 Clients

### Overview

OAuth2 clients are applications authorized to use your authorization server. You need to register each client (web app, mobile app, service) that will request tokens.

### Current Implementation (Database-Backed with YAML Configuration)

The setup uses `JdbcRegisteredClientRepository` for persistent storage. Clients are automatically loaded from `application.yml` on startup and saved to the database.

**Benefits:**
- ✅ No code changes needed to add/modify clients
- ✅ Clients persist in database across restarts
- ✅ Configuration-driven and easy to manage
- ✅ Environment-specific client configurations

### Registering Additional Clients

#### Method 1: Add to application.yml (Recommended)

Simply add new clients to the `app.clients` section in `application.yml`:

```yaml
app:
  clients:
    # Your new client
    - client-id: my-new-app
      client-secret: my-secret
      client-name: My New Application
      client-authentication-methods:
        - CLIENT_SECRET_POST
      authorization-grant-types:
        - AUTHORIZATION_CODE
        - REFRESH_TOKEN
      redirect-uris:
        - https://myapp.com/callback
      scopes:
        - openid
        - profile
        - email
      access-token-time-to-live-seconds: 3600
      refresh-token-time-to-live-seconds: 86400
```

**Important:** 
- Clients are loaded on startup
- If a client with the same `client-id` already exists in the database, it will NOT be overwritten
- To update a client, either delete it from the database or change the `client-id`

### Client Configuration Properties

| Property | Required | Type | Description | Example |
|----------|----------|------|-------------|---------|
| `client-id` | Yes | String | Unique identifier for the client | `"web-app"` |
| `client-secret` | No* | String | Secret for authentication (auto-hashed) | `"my-secret"` |
| `client-name` | No | String | Human-readable name | `"Web Application"` |
| `client-authentication-methods` | No | List | How client authenticates | `["CLIENT_SECRET_POST"]` |
| `authorization-grant-types` | Yes | List | OAuth2 flows allowed | `["AUTHORIZATION_CODE"]` |
| `redirect-uris` | Yes** | List | Redirect URLs after auth | `["https://app.com/callback"]` |
| `scopes` | Yes | List | Permissions client can request | `["openid", "profile"]` |
| `access-token-time-to-live-seconds` | No | Long | Access token expiration | `3600` |
| `refresh-token-time-to-live-seconds` | No | Long | Refresh token expiration | `86400` |
| `require-authorization-consent` | No | Boolean | Show consent screen | `true` |
| `require-proof-key` | No | Boolean | Require PKCE | `true` |

\* Required for confidential clients (web/mobile apps)  
\*\* Required for `AUTHORIZATION_CODE` grant type

### Client Authentication Methods

Available options for `client-authentication-methods`:

- `CLIENT_SECRET_BASIC` - HTTP Basic Authentication
- `CLIENT_SECRET_POST` - Credentials in POST body
- `CLIENT_SECRET_JWT` - JWT signed with client secret
- `PRIVATE_KEY_JWT` - JWT signed with private key
- `NONE` - Public client (no secret)

### Authorization Grant Types

Available options for `authorization-grant-types`:

- `AUTHORIZATION_CODE` - User login with redirect
- `REFRESH_TOKEN` - Refresh access tokens
- `CLIENT_CREDENTIALS` - Service-to-service
- `PASSWORD` - Username/password exchange (not recommended)

### Complete Examples

#### Web Application

```yaml
app:
  clients:
    - client-id: web-app
      client-secret: web-secret-12345
      client-name: Corporate Web Application
      client-authentication-methods:
        - CLIENT_SECRET_POST
        - CLIENT_SECRET_BASIC
      authorization-grant-types:
        - AUTHORIZATION_CODE
        - REFRESH_TOKEN
      redirect-uris:
        - https://myapp.com/oauth2/callback
        - https://myapp.com/authorized
      scopes:
        - openid
        - profile
        - email
        - read
        - write
      access-token-time-to-live-seconds: 3600
      refresh-token-time-to-live-seconds: 604800
```

#### Mobile Application

```yaml
app:
  clients:
    - client-id: mobile-app
      client-secret: mobile-secret-67890
      client-name: Mobile iOS Application
      client-authentication-methods:
        - CLIENT_SECRET_POST
      authorization-grant-types:
        - AUTHORIZATION_CODE
        - REFRESH_TOKEN
      redirect-uris:
        - myapp://oauth2/callback  # Custom URL scheme
      scopes:
        - openid
        - profile
        - email
      access-token-time-to-live-seconds: 1800   # 30 minutes
      refresh-token-time-to-live-seconds: 2592000  # 30 days
```

#### Single Page Application (SPA)

```yaml
app:
  clients:
    - client-id: spa-app
      # No client-secret for public clients
      client-name: React SPA Application
      client-authentication-methods:
        - NONE  # Public client
      authorization-grant-types:
        - AUTHORIZATION_CODE
      redirect-uris:
        - http://localhost:3000/callback
        - https://app.example.com/callback
      scopes:
        - openid
        - profile
      access-token-time-to-live-seconds: 900  # 15 minutes
      require-authorization-consent: true
      require-proof-key: true  # PKCE required for security
```

#### Microservice (Service-to-Service)

```yaml
app:
  clients:
    - client-id: payment-service
      client-secret: service-secret-abc123
      client-name: Payment Processing Service
      client-authentication-methods:
        - CLIENT_SECRET_BASIC
      authorization-grant-types:
        - CLIENT_CREDENTIALS  # No user involved
      scopes:
        - payment.read
        - payment.write
        - invoice.create
      access-token-time-to-live-seconds: 7200  # 2 hours
      # No refresh token for client credentials
```

#### Multiple Clients Example

```yaml
app:
  clients:
    # Production web app
    - client-id: web-prod
      client-secret: ${WEB_CLIENT_SECRET}
      client-name: Production Web App
      client-authentication-methods:
        - CLIENT_SECRET_POST
      authorization-grant-types:
        - AUTHORIZATION_CODE
        - REFRESH_TOKEN
      redirect-uris:
        - https://app.example.com/callback
      scopes:
        - openid
        - profile
        - email
      
    # Admin portal
    - client-id: admin-portal
      client-secret: ${ADMIN_CLIENT_SECRET}
      client-name: Admin Portal
      client-authentication-methods:
        - CLIENT_SECRET_BASIC
      authorization-grant-types:
        - AUTHORIZATION_CODE
        - REFRESH_TOKEN
      redirect-uris:
        - https://admin.example.com/oauth2/callback
      scopes:
        - openid
        - profile
        - admin.read
        - admin.write
      access-token-time-to-live-seconds: 1800
      
    # External API integration
    - client-id: partner-integration
      client-secret: ${PARTNER_CLIENT_SECRET}
      client-name: Partner API Integration
      client-authentication-methods:
        - CLIENT_SECRET_BASIC
      authorization-grant-types:
        - CLIENT_CREDENTIALS
      scopes:
        - api.external.read
      access-token-time-to-live-seconds: 3600
```

### Environment-Specific Configurations

#### Development (application-dev.yml)

```yaml
app:
  clients:
    - client-id: dev-client
      client-secret: dev-secret
      client-name: Development Client
      client-authentication-methods:
        - CLIENT_SECRET_POST
      authorization-grant-types:
        - AUTHORIZATION_CODE
        - REFRESH_TOKEN
      redirect-uris:
        - http://localhost:3000/callback
        - http://localhost:8081/callback
      scopes:
        - openid
        - profile
        - email
      access-token-time-to-live-seconds: 86400  # Long expiry for dev
```

#### Production (application-prod.yml)

```yaml
app:
  clients:
    - client-id: ${PROD_CLIENT_ID}
      client-secret: ${PROD_CLIENT_SECRET}  # From environment variable
      client-name: Production Client
      client-authentication-methods:
        - CLIENT_SECRET_BASIC
      authorization-grant-types:
        - AUTHORIZATION_CODE
        - REFRESH_TOKEN
      redirect-uris:
        - ${PROD_REDIRECT_URI}
      scopes:
        - openid
        - profile
      access-token-time-to-live-seconds: 900  # Short expiry for security
      require-authorization-consent: true
```

### Using Environment Variables

You can use environment variables for sensitive data:

```yaml
app:
  clients:
    - client-id: ${CLIENT_ID:default-client}
      client-secret: ${CLIENT_SECRET:default-secret}
      redirect-uris:
        - ${REDIRECT_URI:http://localhost:3000/callback}
```

Then set the environment variables:

```bash
export CLIENT_ID=my-production-client
export CLIENT_SECRET=super-secret-value-12345
export REDIRECT_URI=https://app.example.com/callback
```

### Best Practices

1. **Use Strong Secrets**
   ```yaml
   client-secret: ${CLIENT_SECRET}  # From secure vault/env var
   ```

2. **Minimal Scopes**
   ```yaml
   scopes:
     - openid
     - profile  # Only what's needed
   ```

3. **Short Token Lifetimes for Production**
   ```yaml
   access-token-time-to-live-seconds: 900  # 15 minutes
   ```

4. **Always Use PKCE for Public Clients**
   ```yaml
   require-proof-key: true
   ```

5. **HTTPS Redirect URIs in Production**
   ```yaml
   redirect-uris:
     - https://app.example.com/callback  # Always HTTPS
   ```

6. **Environment-Specific Configurations**
   - Use `application-dev.yml` for development
   - Use `application-prod.yml` for production
   - Never commit secrets to version control

7. **Client Naming Convention**
   ```yaml
   client-id: service-environment  # e.g., "web-prod", "mobile-dev"
   ```

### Managing Clients

#### Adding a New Client

1. Add the client configuration to `application.yml`
2. Restart the application
3. Client is automatically created in the database

#### Updating a Client

1. Delete the client from the database (if needed)
2. Update the configuration in `application.yml`
3. Restart the application

#### Removing a Client

1. Remove from `application.yml`
2. Manually delete from database (if desired)
3. Or leave in database (won't be recreated on restart)

### Troubleshooting

#### Client Not Created

- Check application logs for errors
- Verify YAML syntax is correct
- Ensure `client-id` is unique
- Check database connectivity

#### Client Already Exists Error

- Client with same `client-id` already in database
- Either delete from database or use different `client-id`

#### Authentication Fails

- Verify `client-secret` matches
- Check `client-authentication-methods` are correct
- Ensure `redirect-uris` match exactly (including protocol and port)

## Adding New Configuration

To add new configuration properties:

1. **Update AppProperties.java**
   ```java
   @Data
   public static class NewFeature {
       private String setting1;
       private int setting2;
   }
   
   private NewFeature newFeature = new NewFeature();
   ```

2. **Add to application.yml**
   ```yaml
   app:
     new-feature:
       setting1: value1
       setting2: 42
   ```

3. **Use in your code**
   ```java
   @Service
   public class MyService {
       private final AppProperties appProperties;
       
       public void doSomething() {
           String setting = appProperties.getNewFeature().getSetting1();
       }
   }
   ```

## Troubleshooting

### Configuration not loading

1. Check the property name matches exactly (case-sensitive)
2. Ensure `@ConfigurationProperties` is present
3. Verify `@Component` or `@EnableConfigurationProperties` is used
4. Check IDE has rebuilt the project

### CORS issues

1. Verify the origin is in `app.cors.allowed-origins`
2. Check protocol (http vs https)
3. Ensure port numbers match
4. Look for trailing slashes

### OAuth2 redirect errors

1. Verify the redirect URI is in `app.oauth2.authorized-redirect-uris`
2. Check exact match including protocol and port
3. Ensure OAuth2 provider has the same redirect URI configured

## Reference

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spring Security OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

