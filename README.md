# 서창동성당 홈페이지

성당 소식, 주보, 본당 사진, 미사 안내, 성경 읽기·통독 진도표와 교우 커뮤니티를 제공하는 Spring Boot 웹 애플리케이션입니다. Thymeleaf로 서버에서 화면을 렌더링하고 PostgreSQL에 데이터를 저장합니다.

## 주요 기능

- 성당 소개, 미사 안내, 매일미사 조회(날짜 탐색·글자 크기 조절) 및 날짜별 인메모리 캐시
- 성경 읽기(주교회의 성경·공동번역 성서) 및 **나의 성경 통독 진도표**(구약 46권 1,068장·신약 27권 260장 장별 읽음 체크, 권 전체 읽음/해제, 진도율 `%` 표시, 로그인 계정 DB 저장 및 비로그인 브라우저 임시 저장)
- 성당 소식·주보·성당현황공개 등록과 첨부파일 관리, 상단 고정(중요 공지)
- **웹 주보 PDF 뷰어**: 성당 소식 상세 화면에서 PDF 첨부파일을 다운로드 없이 바로 열람(한 쪽씩 넘겨보기·연속 스크롤 보기·확대/축소·모바일 좌우 스와이프·다중 PDF 탭 전환)
- 갤러리 대표 사진 지정, 사진 순서 변경, 사진 확대 보기(라이트박스)
- 게시판(자유·사목협의회·자료실), 댓글·답글, 비밀 댓글, 좋아요 및 목록의 댓글 수·첨부 아이콘·NEW·좋아요 수 표시
- 회원가입, 관리자 승인, 임시 비밀번호 초기화, 회원정보·비밀번호 수정
- 공지·댓글·답글·회원 승인 알림, 신규 가입 시 관리자 알림, 읽음 처리, 읽은 알림 삭제
- 성당 일정 조회(FullCalendar), 일정 범례 및 iCalendar(`.ics`) 구독
- 메인 화면 배너·팝업 관리 및 관리자 대시보드(회원·게시판·소식·갤러리·배너·일정 관리)
- Toast UI Editor를 이용한 게시판·성당 소식·갤러리 본문 작성 및 PWA 홈 화면 설치 지원

## 기술 구성

| 구분 | 구성 |
| --- | --- |
| 런타임 | Java 17 |
| 서버 | Spring Boot 3.2.5, Spring MVC, 내장 Tomcat |
| 데이터 | Spring Data JPA, Hibernate, PostgreSQL |
| 화면 | Thymeleaf, Bootstrap 5, Toast UI Editor, FullCalendar, Flatpickr, PDF.js |
| 외부 콘텐츠 | Jsoup 기반 조회 (가톨릭 굿뉴스 매일미사·성경) |
| 이미지 처리 | ImageIO, Thumbnailator |
| 빌드 | Gradle Wrapper 8.14.5, 실행 WAR |
| 테스트 | JUnit 5, Spring Boot Test, MockMvc, H2, Node.js 테스트 |
| 컨테이너 | Docker, Compose, PostgreSQL 16 |

인증은 HTTP 세션을 사용하며, 인터셉터에서 로그인·관리자 권한과 CSRF 토큰을 검사합니다. 비밀번호는 PasswordEncoder로 해시하여 저장합니다.

## 프로젝트 구조

```text
seochang-church/
├── src/
│   ├── main/
│   │   ├── java/com/seochang/church/
│   │   │   ├── config/          # 웹 설정, 세션·권한·CSRF·초기 관리자 처리
│   │   │   ├── controller/      # 화면 및 REST API 요청 처리
│   │   │   ├── dto/             # 데이터 전달 객체
│   │   │   ├── entity/          # JPA 엔티티 (회원, 소식, 게시판, 갤러리, 일정, 알림, 성경진도 등)
│   │   │   ├── repository/      # 데이터 조회·저장 리포지토리
│   │   │   ├── service/         # 비즈니스 로직, 파일·외부 콘텐츠·통독 진도 처리
│   │   │   ├── SeochangChurchApplication.java
│   │   │   └── ServletInitializer.java
│   │   └── resources/
│   │       ├── static/          # CSS, JavaScript, 이미지, PWA(manifest·sw), Toast UI
│   │       ├── templates/       # Thymeleaf 화면
│   │       │   ├── admin/       # 관리자 화면
│   │       │   └── fragments/   # 공통 네비게이션·알림·업로드 진행 조각
│   │       ├── application.properties
│   │       └── application-docker.properties
│   └── test/
│       ├── java/                # Java 단위·통합 테스트
│       └── js/                  # JavaScript 단위 테스트
├── gradle/wrapper/              # Gradle Wrapper
├── uploads/                     # 로컬 업로드 저장소, Git 제외
├── build.gradle
├── init.sql                     # DB 스키마 생성
├── Dockerfile                   # 다단계 이미지 빌드
├── compose.yaml                 # 앱·DB 실행 및 영구 볼륨
├── .env.example                 # Docker 환경변수 예시
└── DOCKER.md                    # 상세 운영·이전·백업 안내
```

## 로컬 실행

### 1. 준비

- 보안 패치가 적용된 JDK 17을 설치하고 `JAVA_HOME`을 설정합니다.
- PostgreSQL을 실행합니다. Docker 구성과 맞추려면 PostgreSQL 16 이상을 사용합니다.
- Gradle은 프로젝트의 Wrapper를 사용하므로 별도 설치하지 않아도 됩니다.
- 의존성 최초 다운로드와 화면의 CDN 리소스 사용에는 인터넷 연결이 필요합니다.

### 2. 데이터베이스 생성

PostgreSQL 관리자 계정으로 접속해 개발용 사용자와 DB를 만듭니다. 아래 비밀번호는 실제 개발용 값으로 변경합니다.

```sql
CREATE USER seochang_church WITH PASSWORD '개발용_비밀번호';
CREATE DATABASE seochang_church OWNER seochang_church;
```

생성한 DB에 `seochang_church` 사용자로 접속하여 스키마를 생성합니다.

```sh
psql -h localhost -U seochang_church -d seochang_church -f init.sql
```

기본 JDBC 주소는 다음과 같습니다.

```text
jdbc:postgresql://localhost:5432/seochang_church?currentSchema=seochang_church_db&sslmode=disable&connectTimeout=30
```

### 3. 로컬 비밀 설정

`src/main/resources/application-secret.properties` 파일을 로컬에 만들고 DB 비밀번호를 설정합니다. 기존 파일이 있으면 해당 설정을 확인합니다.

```properties
spring.datasource.password=개발용_비밀번호
```

DB 주소나 사용자 이름이 다르면 같은 파일에 아래 항목도 지정합니다.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/seochang_church?currentSchema=seochang_church_db&sslmode=disable&connectTimeout=30
spring.datasource.username=seochang_church
```

이 파일은 Git과 Docker 빌드에서 제외됩니다. 비밀번호를 README나 공용 설정에 기록하지 마세요. 로컬 실행은 `.env`를 자동으로 읽지 않으며, `.env`는 Compose용입니다.

### 4. 애플리케이션 실행

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Linux/macOS:

```sh
chmod +x gradlew
./gradlew bootRun
```

브라우저에서 `http://localhost:8080`으로 접속합니다. 최초 실행 시 `ddl-auto=update`에 따라 테이블이 생성됩니다.

## Docker 실행

Docker와 Compose v2가 필요합니다. Windows Docker Desktop은 Linux 컨테이너 모드로 실행합니다.

1. `.env.example`을 `.env`로 복사합니다.
2. `.env`의 `DB_PASSWORD`, `INITIAL_ADMIN_USERNAME`, `INITIAL_ADMIN_PASSWORD`를 설정합니다. 초기 관리자 비밀번호는 12자 이상으로 지정합니다.
3. 프로젝트 루트에서 다음 명령을 실행합니다.

```sh
docker compose config --quiet
docker compose build --pull
docker compose up -d
docker compose ps
docker compose logs --tail=100 app
```

서버에서는 `http://localhost:8080`, 다른 PC에서는 `http://서버IP:8080`으로 접속합니다. `.env.example`과 Compose는 기본적으로 `APP_BIND_ADDRESS=0.0.0.0`을 사용합니다. 서버 방화벽에서 TCP 8080을 허용하고, 환경에 따라 클라우드 보안 그룹이나 공유기 포트포워딩도 설정해야 합니다. DB와 업로드는 명명 볼륨에 보관됩니다.

기존 `.env`에 `APP_BIND_ADDRESS=127.0.0.1`이 있다면 `0.0.0.0`으로 변경하고 `docker compose up -d app`으로 반영하세요. 예제 파일 수정은 기존 `.env`에 자동 반영되지 않으며 이미지 재빌드는 필요하지 않습니다.

같은 호스트의 HTTPS 리버스 프록시를 통해 공개하는 경우에는 `APP_BIND_ADDRESS=127.0.0.1`, `COOKIE_SECURE=true`를 사용하고 프록시의 80·443 포트를 공개합니다.

**기존 로컬 DB와 업로드는 자동으로 이전되지 않습니다.** HTTPS 공개, 데이터 이전, 백업·복원, JDK 업데이트와 중지 방법은 [Docker 운영 안내](DOCKER.md)를 따르세요. `docker compose down -v`는 DB와 업로드 볼륨까지 삭제합니다.

운영 PostgreSQL은 서버의 `127.0.0.1:15432`에만 연결됩니다. 개발 PC의 IntelliJ Database나 DBeaver에서 접근하려면 [SSH 터널을 통한 운영 DB 접속](DOCKER.md#7-개발-pc에서-운영-db-접속)을 참고하세요. DB 포트를 외부에 개방할 필요는 없습니다.

## 관리자 계정 설정

### Docker 최초 설치

`.env`에서 `INITIAL_ADMIN_ENABLED=true`와 초기 관리자 아이디·비밀번호·이름을 지정하면 앱 시작 시 승인된 관리자 계정이 생성됩니다. 활성 관리자가 이미 있으면 생성하지 않으며 기존 계정의 비밀번호나 권한을 덮어쓰지 않습니다. 기존 회원과 아이디가 중복되면 다른 아이디를 지정해야 합니다.

최초 로그인 후 `INITIAL_ADMIN_ENABLED=false`로 변경하고 초기 비밀번호를 `.env`에서 제거한 뒤 `docker compose up -d app`으로 반영하세요. 상세 설정과 검증 조건은 [Docker 운영 안내](DOCKER.md)를 참고하세요.

### 로컬 설치 또는 수동 권한 설정

1. 회원가입 화면에서 관리자용 계정을 생성합니다.
2. DB 운영 권한으로 해당 계정만 승인하고 관리자 권한을 부여합니다.
3. 다시 로그인하여 관리자 페이지에 접속합니다.

```sql
UPDATE seochang_church_db.users
SET role = 'ADMIN', approved = true
WHERE username = '가입한_관리자_아이디' AND del_yn = 'N';
```

로컬 실행에서는 관리자 계정을 자동 생성하지 않습니다. 공통 기본 비밀번호는 제공하지 않으며, 일반 회원은 관리자 승인 후 로그인할 수 있습니다.

## 빌드 및 테스트

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat bootWar
node --test src/test/js/*.cjs
```

Linux/macOS에서는 `./gradlew test`, `./gradlew bootWar`를 사용합니다. JavaScript 테스트에는 Node.js가 필요하며, Docker 이미지 빌드와 `bootWar`만으로는 테스트가 실행되지 않습니다.

현재 설정의 실행 WAR는 `build/libs/church-0.0.1-SNAPSHOT.war`에 생성됩니다.

```sh
java -jar build/libs/church-0.0.1-SNAPSHOT.war
```

WAR 실행 시에도 DB 연결 설정이 필요합니다. 운영에서는 로컬 비밀 설정을 패키징하는 대신 환경변수 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`를 사용하는 방식을 권장합니다. Docker 빌드는 로컬 비밀 설정을 제외합니다.

## 주요 접속 경로 및 REST API

### 화면 경로

| 경로 | 권한 | 설명 |
| --- | --- | --- |
| `/` | 전체 | 메인 (배너·팝업, 최신 소식·갤러리, 미사 시간, 성당 일정, 오시는 길) |
| `/about` | 전체 | 성당 소개 (본당개요·연혁·우리 신부님·구역소개) |
| `/missa` | 전체 | 미사 안내 및 사무실·주일학교·예비자교리 시간 안내 |
| `/daily-missa` | 전체 | 매일미사 (날짜 이동 및 글자 크기 조절) |
| `/bible` | 전체 | 성경 읽기 및 나의 성경 통독 진도표 |
| `/notices` | 전체 (작성·수정은 관리자) | 성당 소식 (공지·주보·성당현황공개 및 웹 주보 PDF 뷰어) |
| `/gallery` | 전체 (작성·수정은 관리자) | 본당 갤러리 |
| `/boards` | 로그인 회원 | 게시판 (자유게시판·사목협의회·자료실) |
| `/calendar` | 전체 | 성당 일정 및 iCalendar 구독 안내 |
| `/signup`, `/login` | 전체 | 회원가입·로그인 |
| `/profile` | 로그인 회원 | 일반 회원 본인 정보·비밀번호 수정 |
| `/notifications` | 로그인 회원 | 내 알림 목록·읽음 처리·삭제 |
| `/admin` | 관리자(`ADMIN`) | 관리자 대시보드 및 회원·게시판·소식·갤러리·배너·일정 관리 |
| `/admin/profile` | 관리자(`ADMIN`) | 관리자 본인 정보·비밀번호 수정 |

### 주요 API 및 파일 엔드포인트

| 메서드·경로 | 권한 | 설명 |
| --- | --- | --- |
| `GET /api/bible` | 전체 | 성경 본문 조회 (`version`, `testament`, `book`, `chapter`) |
| `GET /api/bible/progress` | 전체 | 성경 통독 진도 및 구약·신약·전체 통계 조회 |
| `POST /api/bible/progress/toggle` | 로그인 회원 | 개별 성경 장 읽음 체크/해제 |
| `POST /api/bible/progress/book` | 로그인 회원 | 특정 성경 권 전체 읽음/해제 |
| `POST /api/boards/{id}/comments` | 로그인 회원 | 게시글 댓글·답글·비밀댓글 등록 |
| `DELETE /api/boards/{id}/comments/{commentId}` | 작성자·관리자 | 댓글·답글 삭제 |
| `POST /api/boards/{id}/like` | 로그인 회원 | 게시글 좋아요 토글 |
| `POST /api/images/upload` | 로그인 회원 | Toast UI 에디터 본문 이미지 업로드 |
| `GET /api/calendar/events`, `GET /api/calendar/ics` | 전체 | 일정 JSON 조회 및 iCalendar(`.ics`) 피드 구독 |
| `POST/PUT/DELETE /api/admin/calendar/events/**` | 관리자(`ADMIN`) | 성당 일정 등록·수정·삭제 |
| `GET /api/notifications/unread-count` | 로그인 회원 | 읽지 않은 알림 개수 조회 |
| `GET /download` | 전체 | 첨부파일 다운로드 (`attachment`) |
| `GET /pdf/view` | 전체 | 웹 주보 PDF 인라인 스트리밍 (`inline`, `.pdf` 전용) |

## 데이터베이스 테이블 개요 (`seochang_church_db`)

| 테이블 | 설명 |
| --- | --- |
| `users` | 회원 계정, 이름·세례명, 구역, 권한(`USER`/`ADMIN`), 가입 승인 상태(`approved`), 탈퇴 여부(`del_yn`) |
| `notices`, `notice_attachments` | 성당 소식(공지·주보·현황), 상단 고정(`is_pinned`), 조회수 및 첨부파일 |
| `boards`, `board_attachments` | 게시판(자유·사목협의회·자료실) 게시글 및 첨부파일 |
| `board_comments`, `board_likes` | 게시글 댓글·답글(비밀댓글 포함) 및 게시글 좋아요(`board_id`+`user_id` 유니크) |
| `galleries`, `gallery_attachments` | 본당 갤러리 사진첩 및 사진 목록(정렬 순서 `sort_order`, 대표 사진 `cover_photo`) |
| `calendar_events` | 성당 일정(미사/전례, 행사/모임, 기타/공지 색상 구분) |
| `banners` | 메인 히어로 배너 및 팝업(표시 기간·활성화 상태 관리) |
| `notifications` | 회원별 내부 알림(공지, 댓글, 답글, 가입 승인 대기, 회원 승인 완료) |
| `bible_reading_progress` | 회원별 성경 통독 진도(`user_id`, `testament`, `book`, `chapter` 유니크) |

## 기능별 운영 규칙 참고

### 1. 권한 및 보안 정책
- **CSRF 방어**: 모든 상태 변경 요청(`POST`, `PUT`, `DELETE`)은 세션 기반 동기화 토큰(`_csrf` 파라미터 또는 `X-CSRF-TOKEN` 헤더)을 검증합니다. 화면의 `fetch` 요청은 `static/js/csrf.js`가 자동으로 헤더를 첨부합니다.
- **세션 최신화**: `SessionRefreshInterceptor`가 매 요청마다 로그인 회원의 최신 승인 상태·권한·탈퇴 여부를 확인하여, 탈퇴 처리되거나 미승인 상태인 계정의 세션을 즉시 만료시킵니다.
- **첨부파일 검증**: 이미지는 실제 픽셀 디코딩 및 해상도(2,500만 화소 이하) 검증 후 안전한 JPEG로 재인코딩하여 저장하며, 일반 문서는 허용된 확장자(`pdf`, `hwp`, `hwpx`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `pptx`, `txt`, `csv`, `zip`)만 업로드를 허용합니다.

### 2. 본당 갤러리 사진 관리
- 사진첩당 최소 1장~최대 50장의 사진을 등록할 수 있습니다.
- 사진 순서를 화살표로 변경할 수 있으며, 목록에 노출할 **대표 사진**을 순서와 별개로 지정할 수 있습니다.
- 상세 화면에서는 라이트박스를 통해 키보드 방향키와 모바일 좌우 스와이프로 사진을 넘겨볼 수 있습니다.

### 3. 사이트 내부 알림
- **성당소식 신규 등록**: 승인된 활성 회원 전체에게 알림이 생성됩니다.
- **댓글·답글 등록**: 게시글 작성자와 원댓글 작성자에게 알림이 생성됩니다(본인 및 비밀댓글 열람 권한 없는 회원 제외).
- **회원가입 및 승인**: 신규 회원 가입 시 활성 관리자(`ADMIN`)에게 승인 대기 알림이 발송되며, 관리자가 최초 승인하면 해당 회원에게 승인 완료 알림이 생성됩니다.

## 설정 및 운영 참고

- 기본 포트: `8080`. 환경변수 `SERVER_PORT`로 변경할 수 있습니다.
- 업로드 위치: 로컬 `uploads/`, Docker `/app/uploads/`. `file.upload-dir` 또는 `FILE_UPLOAD_DIR`로 지정합니다.
- 업로드 제한: 파일당 `10MB`, 요청당 `100MB`. 리버스 프록시 제한도 함께 설정해야 합니다.
- 첨부파일 제공: 일반 다운로드는 `/download`, 성당 소식의 PDF 인라인 열람은 `/pdf/view` 엔드포인트를 사용하며 상위 경로 탐색(Path Traversal)과 비허용 확장자를 차단합니다.
- DB 스키마: `seochang_church_db`. 테이블 자동 갱신 전에는 운영 DB를 백업하세요.
- 매일미사·성경 캐시: 서버 메모리에 저장하며 매일미사는 한국 날짜가 바뀐 후 첫 요청에서 초기화됩니다. 서버 재시작 시에도 초기화됩니다.
- 회원 로그인 세션은 서버 메모리를 사용합니다. 재시작 후 다시 로그인해야 합니다.
- 매일미사·성경 외부 조회에는 정상적인 HTTPS 인증서 검증과 네트워크 연결이 필요합니다.
- 배포할 소스에 `static/js`와 `static/toastui`의 필수 파일이 포함되어 있는지 확인하세요. 파일 누락은 빌드 성공 여부와 별개로 화면 기능에 영향을 줍니다.

## 추가 문서

- [Docker 운영·데이터 이전·백업](DOCKER.md)
