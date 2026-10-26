# 서창동성당 홈페이지

성당 소식, 주보, 본당 사진, 미사 안내와 교우 커뮤니티를 제공하는 Spring Boot 웹 애플리케이션입니다. Thymeleaf로 서버에서 화면을 렌더링하고 PostgreSQL에 데이터를 저장합니다.

## 주요 기능

- 성당 소개, 미사 안내, 매일미사 조회 및 날짜별 인메모리 캐시
- 성당 소식·주보·성당현황공개 등록과 첨부파일 관리
- 갤러리 대표 사진 지정, 사진 순서 변경, 사진 확대 보기
- 게시판, 댓글·답글, 비밀 댓글, 좋아요
- 회원가입, 관리자 승인, 회원정보·비밀번호 수정
- 공지·댓글·답글·회원 승인 알림, 읽음 처리, 읽은 알림 삭제
- 성당 일정 조회 및 iCalendar 구독
- 관리자 회원·콘텐츠·일정 관리
- Toast UI를 이용한 게시판·성당 소식·갤러리 본문 작성

## 기술 구성

| 구분 | 구성 |
| --- | --- |
| 런타임 | Java 17 |
| 서버 | Spring Boot 3.2.5, Spring MVC, 내장 Tomcat |
| 데이터 | Spring Data JPA, Hibernate, PostgreSQL |
| 화면 | Thymeleaf, Bootstrap 5, Toast UI Editor |
| 외부 콘텐츠 | Jsoup 기반 조회 |
| 이미지 처리 | Thumbnailator |
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
│   │   │   ├── config/          # 웹 설정, 세션·권한·CSRF 처리
│   │   │   ├── controller/      # 화면 및 API 요청 처리
│   │   │   ├── dto/             # 데이터 전달 객체
│   │   │   ├── entity/          # JPA 엔티티
│   │   │   ├── repository/      # 데이터 조회·저장
│   │   │   ├── service/         # 비즈니스 로직, 파일·외부 콘텐츠 처리
│   │   │   └── SeochangChurchApplication.java
│   │   └── resources/
│   │       ├── bible/          # 성경 관련 리소스
│   │       ├── static/         # CSS, JavaScript, 이미지, Toast UI
│   │       ├── templates/      # Thymeleaf 화면
│   │       │   ├── admin/      # 관리자 화면
│   │       │   └── fragments/  # 공통 메뉴·화면 조각
│   │       ├── application.properties
│   │       └── application-docker.properties
│   └── test/
│       ├── java/               # Java 단위·통합 테스트
│       └── js/                 # JavaScript 테스트
├── gradle/wrapper/             # Gradle Wrapper
├── uploads/                    # 로컬 업로드 저장소, Git 제외
├── build.gradle
├── init.sql                    # DB 스키마 생성
├── Dockerfile                  # 다단계 이미지 빌드
├── compose.yaml                # 앱·DB 실행 및 영구 볼륨
├── .env.example                # Docker 환경변수 예시
└── DOCKER.md                   # 상세 운영·이전·백업 안내
```

## 로컬 실행

### 1. 준비

- 보안 패치가 적용된 JDK 17을 설치하고 `JAVA_HOME`을 설정합니다.
- PostgreSQL을 실행합니다. Docker 구성과 맞추려면 PostgreSQL 16을 사용합니다.
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
jdbc:postgresql://localhost:5432/seochang_church?currentSchema=seochang_church_db
```

### 3. 로컬 비밀 설정

`src/main/resources/application-secret.properties` 파일을 로컬에 만들고 DB 비밀번호를 설정합니다. 기존 파일이 있으면 해당 설정을 확인합니다.

```properties
spring.datasource.password=개발용_비밀번호
```

DB 주소나 사용자 이름이 다르면 같은 파일에 아래 항목도 지정합니다.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/seochang_church?currentSchema=seochang_church_db
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
2. `.env`의 `DB_PASSWORD`를 설정합니다.
3. 프로젝트 루트에서 다음 명령을 실행합니다.

```sh
docker compose config --quiet
docker compose build --pull
docker compose up -d
docker compose ps
docker compose logs --tail=100 app
```

서버에서 `http://localhost:8080`으로 접속할 수 있습니다. 기본적으로 호스트의 `127.0.0.1`에만 포트를 연결합니다. DB와 업로드는 명명 볼륨에 보관됩니다.

**기존 로컬 DB와 업로드는 자동으로 이전되지 않습니다.** HTTPS 공개, 데이터 이전, 백업·복원, JDK 업데이트와 중지 방법은 [Docker 운영 안내](DOCKER.md)를 따르세요. `docker compose down -v`는 DB와 업로드 볼륨까지 삭제합니다.

## 관리자 계정 설정

1. 회원가입 화면에서 관리자용 계정을 생성합니다.
2. DB 운영 권한으로 해당 계정만 승인하고 관리자 권한을 부여합니다.
3. 다시 로그인하여 관리자 페이지에 접속합니다.

```sql
UPDATE seochang_church_db.users
SET role = 'ADMIN', approved = true
WHERE username = '가입한_관리자_아이디' AND del_yn = 'N';
```

기본 관리자 계정과 공통 비밀번호는 자동으로 생성하지 않습니다. 일반 회원은 관리자 승인 후 로그인할 수 있습니다.

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

## 주요 접속 경로

| 경로 | 설명 |
| --- | --- |
| `/` | 메인 |
| `/about` | 성당 소개 |
| `/missa` | 미사 안내 |
| `/daily-missa` | 매일미사 |
| `/bible` | 성경 읽기 |
| `/notices` | 성당 소식 |
| `/gallery` | 본당 갤러리 |
| `/boards` | 게시판, 로그인 필요 |
| `/calendar` | 성당 일정 |
| `/signup`, `/login` | 회원가입·로그인 |
| `/profile` | 일반 회원 본인 정보 수정 |
| `/notifications` | 내 알림 |
| `/admin` | 관리자 페이지 |
| `/admin/profile` | 관리자 본인 정보 수정 |

## 설정 및 운영 참고

- 기본 포트: `8080`. 환경변수 `SERVER_PORT`로 변경할 수 있습니다.
- 업로드 위치: 로컬 `uploads/`, Docker `/app/uploads/`. `file.upload-dir` 또는 `FILE_UPLOAD_DIR`로 지정합니다.
- 업로드 제한: 파일당 `10MB`, 요청당 `100MB`. 리버스 프록시 제한도 함께 설정해야 합니다.
- DB 스키마: `seochang_church_db`. 테이블 자동 갱신 전에는 운영 DB를 백업하세요.
- 매일미사 캐시: 서버 메모리에 저장하며 한국 날짜가 바뀐 후 첫 요청에서 초기화됩니다. 서버 재시작 시에도 초기화됩니다.
- 회원 로그인 세션은 서버 메모리를 사용합니다. 재시작 후 다시 로그인해야 합니다.
- 매일미사·성경 외부 조회에는 정상적인 HTTPS 인증서 검증과 네트워크 연결이 필요합니다.
- 배포할 소스에 `static/js`와 `static/toastui`의 필수 파일이 포함되어 있는지 확인하세요. 파일 누락은 빌드 성공 여부와 별개로 화면 기능에 영향을 줍니다.

## 추가 문서

- [Docker 운영·데이터 이전·백업](DOCKER.md)
- [갤러리 사용 안내](GALLERY.md)
- [알림 기능 안내](NOTIFICATIONS.md)
- [리팩토링 기록](REFACTORING.md)
