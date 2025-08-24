# Authentication & Security Patterns Analysis

## Overview
Comprehensive analysis of the JWT-based authentication system, security patterns, and mobile security considerations for the diary application.

## JWT Token System

### 1. Token Generation
**Implementation**: `pkg/auth/auth.go` - `CreateJWT` function

```go
func CreateJWT(userID, issuer, secret string) (string, error) {
    signingKey := []byte(secret)
    
    claims := &jwt.RegisteredClaims{
        Issuer:    issuer,
        Subject:   userID,
        ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
    }
    
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
    return token.SignedString(signingKey)
}
```

**Token Specifications**:
- **Algorithm**: HMAC-SHA256 (HS256)
- **Expiration**: 24 hours from creation
- **Claims**:
  - `iss` (Issuer): Configurable issuer string
  - `sub` (Subject): User ID (UUID format)
  - `exp` (Expiration): Unix timestamp
- **Signing Key**: Configurable JWT secret (32 characters recommended)

### 2. Token Validation
**Implementation**: `pkg/auth/auth.go` - `CheckJWT` function

```go
func CheckJWT(bearerToken, issuer, jwtSecret string) (string, error) {
    token, err := jwt.Parse(bearerToken, func(token *jwt.Token) (interface{}, error) {
        // Verify signing method is HMAC
        if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
            return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
        }
        return []byte(jwtSecret), nil
    })
    
    if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
        userID, _ := claims.GetSubject()
        actualIssuer, _ := claims.GetIssuer()
        
        if actualIssuer != issuer {
            return "", fmt.Errorf("invalid issuer")
        }
        
        return userID, nil
    }
    
    return "", errors.New("invalid token")
}
```

**Validation Checks**:
- Signature verification using HMAC-SHA256
- Expiration time validation
- Issuer validation
- Token structure validation
- Algorithm verification (prevents algorithm confusion attacks)

### 3. Mobile Token Management Requirements

#### Secure Token Storage
```typescript
// Mobile secure storage implementation
interface TokenManager {
  storeToken(token: string): Promise<void>;
  getToken(): Promise<string | null>;
  clearToken(): Promise<void>;
  isTokenValid(): Promise<boolean>;
  getTokenExpiration(): Promise<Date | null>;
}

// iOS Implementation (Keychain)
class iOSTokenManager implements TokenManager {
  async storeToken(token: string): Promise<void> {
    await Keychain.setInternetCredentials(
      'diary-app-token',
      'user',
      token,
      {
        accessControl: Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
        authenticatePrompt: 'Authenticate to access your diary'
      }
    );
  }
  
  async getToken(): Promise<string | null> {
    try {
      const credentials = await Keychain.getInternetCredentials('diary-app-token');
      return credentials.password;
    } catch (error) {
      return null;
    }
  }
}

// Android Implementation (Keystore)
class AndroidTokenManager implements TokenManager {
  async storeToken(token: string): Promise<void> {
    await EncryptedStorage.setItem('auth_token', token);
  }
  
  async getToken(): Promise<string | null> {
    return await EncryptedStorage.getItem('auth_token');
  }
}
```

## Password Security

### 1. Password Hashing
**Implementation**: `pkg/auth/auth.go` - bcrypt with default cost

```go
func HashPassword(password []byte) ([]byte, error) {
    hashedPassword, err := bcrypt.GenerateFromPassword(password, bcrypt.DefaultCost)
    return hashedPassword, nil
}

func CheckPasswordHash(password, hashedPassword []byte) bool {
    err := bcrypt.CompareHashAndPassword(hashedPassword, password)
    return err == nil
}
```

**Security Features**:
- **Algorithm**: bcrypt with salt
- **Cost Factor**: bcrypt.DefaultCost (currently 10)
- **Salt**: Automatically generated per password
- **Storage**: Base64 encoded in database

### 2. Mobile Password Security

#### Password Validation
```typescript
interface PasswordPolicy {
  minLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireNumbers: boolean;
  requireSpecialChars: boolean;
}

const validatePassword = (password: string, policy: PasswordPolicy): ValidationResult => {
  const errors: string[] = [];
  
  if (password.length < policy.minLength) {
    errors.push(`Password must be at least ${policy.minLength} characters`);
  }
  
  if (policy.requireUppercase && !/[A-Z]/.test(password)) {
    errors.push('Password must contain uppercase letters');
  }
  
  if (policy.requireLowercase && !/[a-z]/.test(password)) {
    errors.push('Password must contain lowercase letters');
  }
  
  if (policy.requireNumbers && !/\d/.test(password)) {
    errors.push('Password must contain numbers');
  }
  
  if (policy.requireSpecialChars && !/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
    errors.push('Password must contain special characters');
  }
  
  return {
    isValid: errors.length === 0,
    errors: errors
  };
};
```

## Authentication Flow

### 1. Login Process
**Web App Flow**: `pkg/server/webapp/login_handler.go`

```
1. User submits email/password form
2. Server validates credentials against database
3. Password verified using bcrypt.CompareHashAndPassword
4. JWT token generated with 24-hour expiration
5. Token stored in HTTP-only session cookie
6. User redirected to original destination
```

**Mobile App Flow Requirements**:
```typescript
const authenticateUser = async (email: string, password: string): Promise<AuthResult> => {
  try {
    // 1. Validate input
    const validation = validateCredentials(email, password);
    if (!validation.isValid) {
      return { success: false, errors: validation.errors };
    }
    
    // 2. Call authentication API
    const response = await apiClient.post('/v1/authorize', {
      email: email.trim().toLowerCase(),
      password: password
    });
    
    // 3. Store token securely
    await tokenManager.storeToken(response.data.token);
    
    // 4. Update authentication state
    authStore.setAuthenticated(true);
    authStore.setUser(await fetchUserProfile());
    
    return { success: true, token: response.data.token };
    
  } catch (error) {
    if (error.response?.status === 401) {
      return { success: false, errors: ['Invalid email or password'] };
    }
    throw error;
  }
};
```

### 2. Session Management
**Web App Session**: Gorilla sessions with HTTP-only cookies

```go
func (r *WebAppRouter) setSessionToken(w http.ResponseWriter, req *http.Request, token string) error {
    session, err := r.cookies.Get(req, r.cfg.CookieName)
    session.Values["token"] = token
    session.Options.Secure = false      // Allow HTTP for local development
    session.Options.SameSite = http.SameSiteLaxMode
    return session.Save(req, w)
}
```

**Mobile Session Management**:
```typescript
class SessionManager {
  private tokenManager: TokenManager;
  private authStore: AuthStore;
  
  async initializeSession(): Promise<boolean> {
    const token = await this.tokenManager.getToken();
    if (!token) return false;
    
    // Validate token before using
    if (await this.isTokenExpired(token)) {
      await this.clearSession();
      return false;
    }
    
    // Set up authenticated state
    this.authStore.setAuthenticated(true);
    return true;
  }
  
  async refreshSession(): Promise<boolean> {
    // Check if token is close to expiration (within 1 hour)
    const token = await this.tokenManager.getToken();
    if (!token) return false;
    
    const expiration = this.getTokenExpiration(token);
    const oneHour = 60 * 60 * 1000;
    
    if (Date.now() + oneHour > expiration.getTime()) {
      // Token expires soon, re-authenticate
      return await this.promptReAuthentication();
    }
    
    return true;
  }
  
  async clearSession(): Promise<void> {
    await this.tokenManager.clearToken();
    this.authStore.setAuthenticated(false);
    this.authStore.clearUser();
  }
}
```

## Authorization Middleware

### 1. Server-Side Authorization
**Implementation**: `pkg/server/middlewares.go`

```go
func AuthMiddleware(logger *slog.Logger, cfg *config.Config) mux.MiddlewareFunc {
    return func(next http.Handler) http.Handler {
        return http.HandlerFunc(func(writer http.ResponseWriter, req *http.Request) {
            // Skip auth for public endpoints
            if req.URL.Path == "/" || strings.HasPrefix(req.URL.Path, "/web/") {
                next.ServeHTTP(writer, req)
                return
            }
            
            // Skip auth for login endpoint
            if req.URL.Path == "/v1/authorize" {
                next.ServeHTTP(writer, req)
                return
            }
            
            checkToken(logger, cfg.Issuer, cfg.JWTSecret, next, writer, req)
        })
    }
}
```

**Token Validation Process**:
1. Extract Authorization header
2. Validate Bearer token format
3. Parse and verify JWT token
4. Extract user ID from token claims
5. Add user ID to request context
6. Continue to protected handler

### 2. Mobile Authorization Middleware

#### API Request Interceptor
```typescript
class APIClient {
  private tokenManager: TokenManager;
  
  constructor() {
    this.setupInterceptors();
  }
  
  private setupInterceptors() {
    // Request interceptor - add auth header
    this.client.interceptors.request.use(async (config) => {
      const token = await this.tokenManager.getToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
    
    // Response interceptor - handle auth errors
    this.client.interceptors.response.use(
      (response) => response,
      async (error) => {
        if (error.response?.status === 401) {
          // Token expired or invalid
          await this.handleAuthError();
          return Promise.reject(new AuthenticationError('Session expired'));
        }
        return Promise.reject(error);
      }
    );
  }
  
  private async handleAuthError() {
    await this.tokenManager.clearToken();
    // Navigate to login screen
    NavigationService.navigate('Login');
  }
}
```

#### Route Protection
```typescript
const AuthGuard: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  
  if (isLoading) {
    return <LoadingScreen />;
  }
  
  if (!isAuthenticated) {
    return <LoginScreen />;
  }
  
  return <>{children}</>;
};

// Usage in navigation
const AppNavigator = () => (
  <Stack.Navigator>
    <Stack.Screen name="Login" component={LoginScreen} />
    <Stack.Screen name="Protected">
      {() => (
        <AuthGuard>
          <ProtectedScreens />
        </AuthGuard>
      )}
    </Stack.Screen>
  </Stack.Navigator>
);
```

## Security Considerations

### 1. Token Security
**Current Implementation**:
- 24-hour token expiration
- HMAC-SHA256 signing
- Issuer validation
- Algorithm verification

**Mobile Enhancements Needed**:
- Secure storage (Keychain/Keystore)
- Biometric authentication
- Token refresh mechanism
- Certificate pinning for API calls

### 2. Network Security

#### HTTPS Enforcement
```typescript
const secureAPIClient = axios.create({
  baseURL: 'https://api.diary.app',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Certificate pinning (React Native)
const certificatePinning = {
  hostname: 'api.diary.app',
  sslPinning: {
    certs: ['sha256/CERTIFICATE_HASH']
  }
};
```

#### Request Security
```typescript
const securityHeaders = {
  'X-Requested-With': 'XMLHttpRequest',
  'X-App-Version': getAppVersion(),
  'X-Device-ID': getDeviceId(),
  'User-Agent': getUserAgent()
};
```

### 3. Data Protection

#### Local Data Encryption
```typescript
class SecureStorage {
  private encryptionKey: string;
  
  async storeSecureData(key: string, data: any): Promise<void> {
    const encrypted = await this.encrypt(JSON.stringify(data));
    await AsyncStorage.setItem(key, encrypted);
  }
  
  async getSecureData(key: string): Promise<any> {
    const encrypted = await AsyncStorage.getItem(key);
    if (!encrypted) return null;
    
    const decrypted = await this.decrypt(encrypted);
    return JSON.parse(decrypted);
  }
  
  private async encrypt(data: string): Promise<string> {
    // Use platform-specific encryption
    return CryptoJS.AES.encrypt(data, this.encryptionKey).toString();
  }
  
  private async decrypt(encryptedData: string): Promise<string> {
    const bytes = CryptoJS.AES.decrypt(encryptedData, this.encryptionKey);
    return bytes.toString(CryptoJS.enc.Utf8);
  }
}
```

### 4. Biometric Authentication

#### Implementation Strategy
```typescript
interface BiometricAuth {
  isAvailable(): Promise<boolean>;
  authenticate(reason: string): Promise<boolean>;
  storeCredentials(credentials: any): Promise<void>;
  getCredentials(): Promise<any>;
}

class BiometricAuthManager implements BiometricAuth {
  async isAvailable(): Promise<boolean> {
    const biometryType = await TouchID.isSupported();
    return biometryType !== false;
  }
  
  async authenticate(reason: string): Promise<boolean> {
    try {
      await TouchID.authenticate(reason, {
        fallbackLabel: 'Use Passcode',
        unifiedErrors: false,
        passcodeFallback: true
      });
      return true;
    } catch (error) {
      return false;
    }
  }
  
  async enableBiometricLogin(): Promise<void> {
    const isAuthenticated = await this.authenticate('Enable biometric login');
    if (isAuthenticated) {
      await SecureStore.setItemAsync('biometric_enabled', 'true');
    }
  }
}
```

## Error Handling & Security Events

### 1. Authentication Error Handling
```typescript
enum AuthErrorType {
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',
  NETWORK_ERROR = 'NETWORK_ERROR',
  BIOMETRIC_FAILED = 'BIOMETRIC_FAILED',
  ACCOUNT_LOCKED = 'ACCOUNT_LOCKED'
}

class AuthErrorHandler {
  handle(error: AuthError): void {
    switch (error.type) {
      case AuthErrorType.INVALID_CREDENTIALS:
        this.showError('Invalid email or password');
        break;
      case AuthErrorType.TOKEN_EXPIRED:
        this.redirectToLogin('Your session has expired');
        break;
      case AuthErrorType.NETWORK_ERROR:
        this.showRetryOption('Network error. Please try again.');
        break;
      case AuthErrorType.BIOMETRIC_FAILED:
        this.fallbackToPassword();
        break;
    }
  }
}
```

### 2. Security Event Logging
```typescript
interface SecurityEvent {
  type: string;
  timestamp: Date;
  userId?: string;
  deviceId: string;
  ipAddress?: string;
  userAgent: string;
  details: any;
}

class SecurityLogger {
  logAuthAttempt(success: boolean, email: string): void {
    this.log({
      type: success ? 'AUTH_SUCCESS' : 'AUTH_FAILURE',
      timestamp: new Date(),
      details: { email, success }
    });
  }
  
  logTokenRefresh(userId: string): void {
    this.log({
      type: 'TOKEN_REFRESH',
      timestamp: new Date(),
      userId,
      details: {}
    });
  }
  
  logSecurityViolation(violation: string, details: any): void {
    this.log({
      type: 'SECURITY_VIOLATION',
      timestamp: new Date(),
      details: { violation, ...details }
    });
  }
}
```

## Mobile Security Best Practices

### 1. App Security
- Enable app transport security (ATS)
- Implement certificate pinning
- Use secure storage for sensitive data
- Enable code obfuscation
- Implement jailbreak/root detection
- Use secure communication protocols

### 2. Runtime Security
- Implement anti-tampering measures
- Monitor for debugging attempts
- Validate app integrity
- Implement secure logging
- Handle app backgrounding securely

### 3. Data Security
- Encrypt local databases
- Secure API communications
- Implement proper key management
- Use secure random number generation
- Implement secure data deletion
