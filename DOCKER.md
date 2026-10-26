# Docker 운영 안내

애플리케이션(Java 17)과 PostgreSQL 16을 Compose로 실행합니다. Docker Engine/Desktop과 Compose v2가 필요합니다. Windows는 Linux 컨테이너 모드를 사용하세요.

## 1. 최초 실행

프로젝트 루트에서 `.env.example`을 `.env`로 복사하고 `DB_PASSWORD`와 초기 관리자 정보를 지정합니다. DB와 관리자 비밀번호는 서로 다른 고유 값으로 설정하세요. 비밀번호에 `$`가 있다면 `.env`에서 값을 작은따옴표로 감싸세요. `.env`는 서버 운영 계정만 읽을 수 있도록 권한을 제한합니다.

```dotenv
INITIAL_ADMIN_ENABLED=true
INITIAL_ADMIN_USERNAME=admin
INITIAL_ADMIN_PASSWORD='직접_정한_12자_이상_비밀번호'
INITIAL_ADMIN_NAME=관리자
```

위 비밀번호는 형식 예시이므로 그대로 사용하지 마세요. 아이디는 영문·숫자·`_`·`.`·`-` 4~50자, 비밀번호는 12자 이상·UTF-8 기준 72바이트 이하입니다.

```powershell
Copy-Item .env.example .env
```

```sh
docker compose config --quiet
docker compose build --pull
docker compose up -d
docker compose ps
docker compose logs --tail=100 app
```

서버 자체에서는 `http://localhost:8080`, 다른 PC에서는 `http://서버IP:8080`으로 접속합니다. 기본 바인딩은 `0.0.0.0`으로, 모든 IPv4 인터페이스에서 접속을 받습니다. `0.0.0.0`은 브라우저 접속 주소가 아니므로 실제 서버 IP를 사용하세요.

외부 접속에는 서버 방화벽의 TCP 8080 허용이 필요합니다. 클라우드 서버는 보안 그룹, 공유기 내부 서버는 포트포워딩도 확인하세요. `APP_PORT`를 변경했다면 해당 포트를 허용하고 접속 주소에도 반영합니다.

기존 `.env`는 예제 파일을 변경해도 자동으로 갱신되지 않습니다. 기존 `.env`의 비밀번호 등은 유지하고 아래 값만 변경한 후 컨테이너를 재생성합니다.

```dotenv
APP_BIND_ADDRESS=0.0.0.0
APP_PORT=8080
```

```sh
docker compose up -d app
docker compose ps
```

이미지 재빌드는 필요하지 않습니다. `docker compose ps`에서 `0.0.0.0:8080->8080/tcp` 포트 매핑을 확인하세요. `docker compose restart`만으로는 변경한 포트 매핑이 반영되지 않습니다.

DB 준비 완료 후 앱이 시작됩니다. 첫 실행에만 `init.sql`로 스키마를 만들고, Hibernate가 테이블을 생성·갱신합니다. 기존 프로젝트의 `ddl-auto=update`를 유지하므로 운영 업데이트 전에 반드시 백업하세요. PostgreSQL 포트는 호스트에 공개하지 않습니다.

이미지에는 `.env`, 로컬 비밀 설정, 기존 업로드와 DB가 포함되지 않습니다. 애플리케이션은 일반 사용자 UID/GID 10001로 실행됩니다. 빌드는 실행 WAR를 생성하며 테스트는 별도 실행합니다.

## 2. 기존 운영 데이터 이전

새 Compose DB는 빈 DB입니다. 기존 회원·게시글·사진이 자동으로 이전되지 않습니다.

1. 기존 PostgreSQL 버전을 확인하고 호환되는 `pg_dump`로 백업합니다. PostgreSQL 16보다 새로운 서버라면 Compose DB 버전도 맞춰야 합니다.
2. 기존 사이트의 쓰기를 중지한 상태에서 DB와 `uploads`를 함께 백업합니다.
3. 새 DB를 시작하고 복원한 뒤, 업로드 파일을 옮깁니다.

```sh
docker compose up -d db
docker compose cp backups/church.dump db:/tmp/church.dump
docker compose exec db pg_restore -U seochang_church -d seochang_church --no-owner --no-privileges /tmp/church.dump
```

복원은 빈 운영 대상 DB에서 진행합니다. 오류가 나면 앱을 시작하기 전에 원인을 확인하세요. 기존 DB와 사용자 이름은 백업에 맞게 조정합니다.

업로드 볼륨 생성 후 기존 업로드 폴더의 **내용**을 복사합니다. 다음 예시는 프로젝트 루트의 `uploads`를 사용합니다.

```sh
docker compose create app
docker compose cp uploads/. app:/app/uploads/
docker compose run --rm --no-deps --user root --entrypoint chown app -R 10001:10001 /app/uploads
docker compose up -d app
```

### 초기 관리자 자동 생성

Docker 프로필에서 `INITIAL_ADMIN_ENABLED=true`일 때, 승인된 활성 관리자가 없으면 지정한 계정을 생성합니다. 비밀번호는 BCrypt 해시로 저장하며 로그에 출력하지 않습니다. 생성된 계정은 별도 승인 없이 로그인할 수 있습니다.

- 활성 관리자가 이미 있으면 새 계정을 만들거나 기존 비밀번호를 변경하지 않습니다.
- 지정한 아이디가 기존 회원과 중복되면 시작을 중단합니다. 기존 회원을 자동 승격하지 않으므로 다른 아이디를 지정하세요.
- 관리자가 없고 초기 생성 정보가 잘못되면 오류 메시지와 함께 시작을 중단합니다.
- 기존 `.env`에는 설정이 자동으로 추가되지 않습니다. 기능을 사용하려면 위 네 항목을 직접 추가하세요.
- 최초 로그인 확인 후 `.env`에서 `INITIAL_ADMIN_ENABLED=false`로 바꾸고 `INITIAL_ADMIN_PASSWORD`를 비운 다음 `docker compose up -d app`으로 반영하세요. 이후 비밀번호 변경은 관리자 페이지의 내 회원정보 수정을 이용합니다.
- 초기 생성 설정을 비활성화하면 재시작이나 데이터 복원 시 관리자를 자동 생성하지 않습니다.

공통 기본 비밀번호는 제공하지 않습니다. 기존 회원을 직접 관리자로 지정해야 하는 경우에는 아래 수동 절차를 사용할 수 있습니다.

```sh
docker compose exec db psql -U seochang_church -d seochang_church
```

```sql
UPDATE seochang_church_db.users
SET role = 'ADMIN', approved = true
WHERE username = '가입한_관리자_아이디' AND del_yn = 'N';
```

변경 후 다시 로그인합니다.

## 3. HTTPS 연결

도메인과 TLS 인증서는 서버의 Nginx/Caddy 등 리버스 프록시에서 처리합니다. 호스트 프록시는 `127.0.0.1:8080`으로 전달하고 `Host`, `X-Forwarded-For`, `X-Forwarded-Proto` 헤더를 설정합니다. 컨테이너 프록시는 별도 네트워크 구성이 필요합니다.

- 같은 호스트의 프록시를 통한 공개 HTTPS 운영 시 `.env`에 `APP_BIND_ADDRESS=127.0.0.1`, `COOKIE_SECURE=true`를 설정하고 `docker compose up -d app`으로 반영합니다. 이 구성의 외부 공개 포트는 프록시의 80·443이며, 앱의 8080은 내부 연결에 사용합니다.
- Nginx의 `client_max_body_size`는 최소 `100m`, 업로드 시간 제한은 서비스 환경에 맞게 설정합니다.
- 외부에서 앱 포트에 직접 접근하지 못하도록 바인딩과 방화벽을 유지합니다.

## 4. 업데이트 및 JDK 보안 패치

코드 변경을 반영하기 전에 DB·업로드를 백업합니다.

```sh
docker compose build --pull --no-cache app
docker compose up -d --no-deps app
docker compose ps
docker compose logs --tail=100 app
docker compose exec app java -version
```

`docker compose pull`만으로는 직접 빌드한 앱 내부 JRE가 갱신되지 않습니다. 기본 이미지를 다시 받아 재빌드해야 합니다. Java 17이라는 태그만으로 특정 CVE 수정 여부를 보장하지 않으므로 실제 실행 버전과 배포판 보안 공지를 확인하세요. 검증한 이미지 digest와 `APP_IMAGE_TAG`를 운영 이력에 기록하면 재현과 롤백에 도움이 됩니다.

컨테이너 재생성 시 로그인 세션과 매일미사 메모리 캐시는 초기화됩니다. 업로드와 DB는 볼륨에 유지됩니다. 단일 앱 인스턴스 운영을 전제로 합니다.

## 5. 백업과 복구

백업 시점의 DB와 업로드를 맞추기 위해 앱을 잠시 중지합니다. `backups` 폴더를 먼저 만드세요. 백업은 별도 저장소에도 보관합니다.

```sh
docker compose stop app
docker compose exec db pg_dump -U seochang_church -d seochang_church -Fc -f /tmp/church.dump
docker compose cp db:/tmp/church.dump backups/church.dump
```

업로드 백업은 다음 명령으로 호스트에 직접 저장하세요 (이 명령만 **Linux 셸 또는 WSL**에서 실행).

```sh
docker compose run --rm --no-deps -T --user root --entrypoint tar app -czf - -C /app/uploads . > backups/uploads.tar.gz
docker compose start app
```

Windows PowerShell에서는 바이너리 리다이렉션 대신 다음 방식으로 복사합니다.

```powershell
docker compose cp app:/app/uploads backups/uploads
docker compose start app
```

백업 파일명은 날짜별로 변경해 이전 백업을 덮어쓰지 마세요. DB 복원은 위 이전 절차와 동일하며, 업로드 백업은 별도 테스트 환경에서 풀어 복구를 검증한 뒤 운영에 적용합니다.

## 6. 중지와 점검

```sh
docker compose stop
docker compose start
docker compose logs --tail=100 app db
```

`docker compose down`은 컨테이너를 제거하지만 명명 볼륨은 유지합니다. **`docker compose down -v`는 DB와 업로드 볼륨까지 삭제하므로 운영에서 사용하지 마세요.** `.env`에서 DB 비밀번호를 바꿔도 기존 DB 비밀번호는 변경되지 않습니다. DB에서 비밀번호를 변경한 뒤 앱 설정을 함께 반영해야 합니다.

현재 작업 환경에는 Docker가 설치되어 있지 않아 이미지 빌드·컨테이너 기동·백업 복원은 검증하지 못했습니다. 운영 전 별도 환경에서 로그인, 게시글, 사진 업로드, 다운로드, 매일미사·성경 HTTPS 연결을 확인하세요.

작성 시점 작업 폴더에는 기존 JavaScript와 Toast UI JavaScript 파일 일부가 삭제된 상태입니다. Docker는 현재 소스를 그대로 패키징하므로 배포할 소스에 필요한 정적 파일이 모두 있는지 확인해야 합니다. 이번 Docker 구성 작업에서는 해당 파일을 복구하거나 변경하지 않았습니다.
