# mini-facebook

Mạng xã hội thu nhỏ: React 19 + Spring Boot 4 + MySQL 8.4. Quy tắc phát triển: [CLAUDE.md](CLAUDE.md).

## Yêu cầu

- JDK 21+ (Temurin 21 khuyến nghị), Maven 3.9+
- Node 22 LTS trở lên
- Docker Desktop + WSL2 (MySQL dev và Testcontainers)
- [lefthook](https://lefthook.dev) (cài qua `npm install` trong `frontend/`) và
  [gitleaks](https://github.com/gitleaks/gitleaks) (`winget install Gitleaks.Gitleaks`)

## Chạy lần đầu

```bash
# 1. Biến môi trường
cp .env.example .env              # điền DB_PASSWORD, DB_ROOT_PASSWORD, JWT_SECRET, MEDIA_SIGNING_SECRET
                                  # bí mật: openssl rand -base64 32

# 2. MySQL
docker compose up -d

# 3. Backend (http://localhost:8080, Swagger: /swagger-ui.html ở profile dev)
cd backend
set -a && . ../.env && set +a     # PowerShell: xem mục "Windows" bên dưới
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Frontend (http://localhost:5173, proxy /api → 8080)
cd frontend
npm install
npm run dev

# 5. Git hook (một lần sau khi clone)
npx --prefix frontend lefthook install
```

### Windows (PowerShell) — nạp `.env`

```powershell
Get-Content ..\.env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v
}
```

## Kiểm tra

```bash
cd backend && mvn verify          # Spotless, SpotBugs+FindSecBugs, test (Testcontainers), JaCoCo
cd frontend && npm run check      # tsc, ESLint, Prettier, Vitest
```
