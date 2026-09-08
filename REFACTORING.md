# 리팩토링 검증 기록

## 변경 사항

- 게시판·공지·갤러리 입력을 PostForm으로 제한했습니다. ID, 작성자, 삭제 상태와 카운트는 폼에서 엔티티로 전달되지 않습니다.
- 세션의 회원 상태를 요청마다 새로 조회합니다. 탈퇴·미승인 계정은 세션을 만료하고 권한 변경을 반영합니다. 로그인 성공 시 세션 ID를 교체합니다.
- HTML POST 폼과 같은 출처의 fetch 요청에 세션 기반 CSRF 토큰을 적용했습니다. 로그아웃은 POST로 변경했습니다. 외부 API 클라이언트도 세션과 X-CSRF-TOKEN이 필요합니다.
- 이미지 업로드 API에 로그인 검사를 추가했습니다. 이미지 데이터를 실제로 읽어 검증하고 JPEG로 재인코딩합니다. 이미지 최대 크기는 10MB, 2,500만 화소입니다.
- 일반 첨부파일은 pdf, txt, csv, doc/docx, xls/xlsx, ppt/pptx, hwp/hwpx, zip 확장자만 허용합니다. 이 제한은 악성코드 검사를 대신하지 않습니다.
- 첨부 개수·파일 형식을 변경 전에 검사합니다. 게시글 생성·수정·삭제를 트랜잭션으로 묶고 새 파일은 롤백 시 정리하며 기존 파일 삭제는 커밋 후 실행합니다. 프로세스 강제 종료나 디스크 오류까지 원자적으로 복구하는 구조는 아닙니다. 삭제 실패는 로그에 남습니다.
- 좋아요 처리를 서비스로 옮기고 게시글 행 잠금과 단일 트랜잭션을 적용했습니다. 조회수는 DB에서 직접 증가시키며 게시글은 변경된 필드만 UPDATE합니다.
- 외부 사이트 인증서 검증 우회를 제거하고 매일미사 HTML을 허용 목록으로 정제합니다.

## 검증

- Java 17 / Gradle 8.14.5에서 `gradlew.bat test bootWar --offline --no-daemon` 성공.
- H2 테스트 DB에서 Java 테스트 17개 통과: 입력 조작, 세션 갱신, CSRF, 익명 업로드, 좋아요, 템플릿 렌더링, 파일 검증·정리 등.
- `node --test src/test/js/security.test.cjs`: JavaScript CSRF 테스트 통과.
- WAR: build/libs/church-0.0.1-SNAPSHOT.war
- 실제 PostgreSQL의 동시 요청 부하, 브라우저 종단 간 동작, 외부 성경·매일미사 연결은 검증하지 않았습니다.
- 운영 DB 연결이나 배포는 수행하지 않았습니다. 기존 daily_mass.html~ 파일은 변경하지 않았습니다.

## 2026-09-08: 매일미사 PKIX 오류 수정

- 실제 연결 진단에서 JVM 기본 인증서는 PKIX 오류로 실패하고, Windows-ROOT 인증서는 HTTP 200으로 성공했습니다. 해당 네트워크의 서버 인증서 발급자는 Somansa Root CA였습니다.
- CatholicHttpClient가 성경·매일미사 HTTPS 요청을 공통 처리합니다. Windows에서는 JVM 신뢰 인증서에 기존 Windows-ROOT 인증서를 더한 전용 SSLContext를 사용합니다. 인증서 검증과 호스트명 검증을 유지하며 JVM 전역 설정이나 인증서 저장소는 변경하지 않습니다.
- `catholic.http.use-windows-root=true`가 기본값입니다. `false`로 설정하면 JVM 신뢰 저장소만 사용합니다. Windows가 아닌 환경에서는 JVM 저장소를 사용합니다. 별도의 서비스 계정으로 실행할 경우 해당 계정의 Windows 인증서 신뢰 구성을 확인해야 합니다.
- 연결 대상을 `https://maria.catholic.or.kr`로 제한하고 자동 리다이렉트를 끕니다. 신뢰되지 않는 인증서를 자동으로 가져오거나 검증 없이 허용하지 않습니다.
- 적용하려면 애플리케이션을 재시작해야 합니다. Linux 또는 Windows 인증서 저장소가 없는 서버의 경우, 운영자가 확인한 조직 CA를 별도 JVM trust store에 설정해야 합니다.
- 개발자 참고: [Oracle Java 17 SunMSCAPI / Windows-ROOT 문서](https://docs.oracle.com/en/java/javase/17/security/oracle-providers.html).
- 실제 외부 연결 테스트는 `CATHOLIC_LIVE_TEST=true` 환경 변수로 명시적으로 실행합니다. 기본 테스트는 인터넷 연결을 요구하지 않습니다.
- 이번 수정 검증: Java 테스트 22개 모두 통과(실제 매일미사·성경 조회 포함, 건너뛴 테스트 없음). JVM/Windows 신뢰 설정 모두에서 임의 자체서명 인증서를 거부하는 테스트도 통과했습니다. WAR 빌드가 성공했습니다.
