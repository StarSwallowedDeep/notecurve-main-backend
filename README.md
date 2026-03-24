<p align="center">
  <img src="https://github.com/user-attachments/assets/113caf7b-9c91-4fa4-86c8-b99d2964e8b0" />
</p>

# **NOTECURVE**
## **📑 개요**
<br>

+ **소개**
    + ### **" 기술 블로그 및 개인 노트 플랫폼 "**
      **기술 경험과 인사이트를 공유**할 수 있는 블로그와, **개인 학습 내용을 기록**할 수 있는 노트를 제공하는 플랫폼입니다.
      <br>Java 소스코드 정적 분석(AST) 기반 아키텍처 시각화 기능(FlowMap)을 제공합니다.

<br>

+ **목적**
  
    + **체계적 기록**: 새로운 기술을 탐구하거나 프로젝트를 진행하면서 얻는 **다양한 경험과 배움을 한 곳**에 체계적으로  
    기록할 수 있도록 합니다.

    + 사고 과정 · 기술적 통찰 · 구현 경험을 축적하여 필요할 때 다시 참고할 수 있는 **개인 지식 저장소**를 구축하고,  
    이를 통해 **기술 확장과 아이디어 탐색**을 자연스럽게 이어갈 수 있는 기술 아카이브 플랫폼을 만드는 것을 목표로 합니다.  
<br>

## **🔗 주요 기능**
<br>

+ **주요 기능**

    + **기술 블로그** | 게시글 CRUD, MCP 기반 게시글 요약 및 관련 글 정리
    + **개인 노트** | 계정 기반 접근 제어, 노트 CRUD, 파일·이미지·문서 첨부
    + **사용자 관리** | 회원가입, JWT 로그인/로그아웃, 비밀번호·이름 변경, 회원탈퇴
    + **FlowMap** | Java 소스코드 업로드 시 클래스 의존성 인터랙티브 다이어그램 제공
    + **Admin** | 콘텐츠 및 사용자 통합 관리

<br>

## **📌 아키텍처 구성**
<br>

+ **시스템 아키텍처**
    <p>
      <img width="1812" height="985" src="https://github.com/user-attachments/assets/87ebd251-8f76-4f7d-99c6-d7ca3d50f1ec" style="width: 80%"/>
    </p>
<br>

+ **기술 스택**  

  + **Frontend**  
  ![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=white)
  ![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
  ![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwind-css&logoColor=white)
  + **Backend**  
    ![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)
    ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
  + **Database**  
    ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
    ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
  + **Message Queue**  
    ![Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
  + **DevOps**  
    ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
    ![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
    ![Terraform](https://img.shields.io/badge/Terraform-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)
  + **Cloud**  
    ![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)
<br>

## **🔧 구현 과정**
<br>

1. **인증/인가 처리 방식 (Spring Security + JWT)**
    + 기존 파라미터 기반 인증 방식의 보안 취약점을 발견하고 Spring Security 기반 인증/인가 구조로 재설계
    + JWT에서 사용자 정보(loginId)를 파싱 -> DB 조회 후 UserDetailsImpl 생성 -> SecurityContextHolder에 저장
    + 컨트롤러에서 SecurityContextHolder를 통해 현재 로그인된 사용자 조회
      ```java
      private User getCurrentUser() {
          UserDetailsImpl userDetails = 
              (UserDetailsImpl) SecurityContextHolder.getContext()
                  .getAuthentication()
                  .getPrincipal();

          return userDetails.getUser();
      }
    + 서비스 계층에서 사용자 권한 검증
      ```java
      if (!category.getUser().getId().equals(user.getId())) {
          throw new RuntimeException("Unauthorized access to category");
      }
<br>

2. **N+1 문제 해결 (JOIN FETCH)**
    + 게시글(Post) 조회 기능에서 각 게시글은 작성자(User) 정보를 참조
    + 기본 상태(LAZY 로딩)에서는 게시글 10개를 조회할 경우, 각 게시글마다 User 정보를 추가로 조회하는 쿼리가 실행되는 문제 발생
    + 이를 해결하기 위해 JPQL의 JOIN FETCH를 이용하여 게시글과 작성자를 한 번의 쿼리로 함께 조회하도록 개선
      ```java
      @Query("SELECT p FROM Post p JOIN FETCH p.user")
      List<Post> findAllWithUser();

      @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :postId")
      Optional<Post> findByIdWithUser(@Param("postId") Long postId);
<br>

3. **트랜잭션 정합성 보장 (afterCommit)**
    + 게시글 수정 과정에서 DB에 저장된 이미지 정보 변경과 서버 파일 시스템의 실제 이미지 삭제가 함께 이루어져야 함
    + 트랜잭션은 DB에만 적용되므로, 파일 시스템에서 수행된 삭제나 생성 작업은 롤백되지 않아 DB와 파일 상태가 불일치할 수 있음
    + Spring의 TransactionSynchronizationManager.registerSynchronization를 사용하여 트랜잭션 커밋 성공 후에만 실제 파일을 삭제하도록 구현
      ```java
      // ========================= 텍스트 및 이미지 반영 =========================
      post.updateFrom(postRequestDto, finalImages);
      Post updatedPost = postRepository.save(post);

      // ========================= 본문 이미지 파일 삭제 (커밋 이후) =========================
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCommit() {
              imagesToDelete.forEach(img -> deleteFile(img.getContentImageUrl()));
          }
      });
<br>

4. **Redis 캐시 성능 최적화**
    + 게시글 100개·100명 동시 요청 환경에서 매번 DB를 조회하는 구조로 인해 평균 응답시간 1,330ms 병목 발생
    + Cache-Aside 전략 기반 Redis 캐시 도입
    <br><br>

      **지표 결과**
      <table>
        <tr>
          <td align="center"><b>도입 전</b></td>
          <td align="center"><b>도입 후</b></td>
        </tr>
        <tr>
          <td align="center"><img src="https://github.com/user-attachments/assets/9482ca4e-4dbc-4d93-b8fc-3ac03bdda8b8" /></td>
          <td align="center"><img src="https://github.com/user-attachments/assets/6c395d06-4dc4-480d-a438-e42086274897" /></td>
        </tr>
      </table>  

      <br>**추가 고려사항**
    + **캐시 스탬피드 문제**: TTL에 25~35분 랜덤 Jitter 적용하여 만료 시점 분산
    + **캐시 무효화**: 게시글 수정·삭제 시 @CacheEvict로 캐시 무효화
    + **장애 대응**: Redis 장애 시 DB Fallback으로 가용성 확보

<br><br>

5. **MSA 분리 + Kafka + CQRS**
    + 모놀리식 구조에서 메인 서비스 장애와 부하가 관리자 서비스로 전파되는 문제 발생
    + 관리자 통계 조회 시 메인 서버에 매번 N회 실시간 API를 호출하는 강결합 구조로 인한 성능 한계
    <br><br>
    
      **해결**
    + MSA로 서비스 분리, DB 독립 구성으로 장애 격리
    + Kafka 기반 이벤트 드리븐 아키텍처 도입
    + CQRS 패턴으로 어드민 전용 DB에 통계 사전 집계
    <br><br>

      **Kafka 이벤트 흐름**
      ```
      Main-Server → 이벤트 발행 → Kafka
      Kafka → 이벤트 구독 → Admin-Server
      Admin-Server → 통계 저장 → Admin DB
      ```
      
      <br>**트러블슈팅**
    + Kafka 멀티 토픽 환경에서 비동기 처리 특성으로 게시판 삭제 시 연관 댓글 삭제 이벤트가 누락되어 어드민 통계 불일치 문제 발생
    + → 게시판 삭제 이벤트 단일 수신 후 어드민 DB에서 연관 댓글 수를 직접 집계하는 원자적 처리 방식으로 해결
<br><br>

6. **멱등성 처리**
    + 게시글 빠른 연속 클릭으로 인한 중복 생성 문제 발생
    <br><br>

      **해결 흐름**
      ```
      클라이언트 → UUID 생성 → X-Idempotency-Key 헤더에 담아 전송
      서버 → Redis에 UUID 존재 여부 확인
            → 있으면 중복 요청으로 판단 → 차단
            → 없으면 Redis에 UUID 저장 (TTL 5분) → 정상 처리
      ```
<br><br>

7. **FlowMap - Java 아키텍처 시각화**
    + Java 소스코드를 업로드하면 클래스 간 의존성을 인터랙티브 다이어그램으로 시각화하는 기능
    <br><br>

      **동작 원리**
      ```
      1. 사용자가 Java 소스코드 업로드
      2. JavaParser(AST)로 클래스·어노테이션·의존성 정적 분석
      3. 분석 결과를 nodes/edges JSON으로 변환
      4. React Flow로 인터랙티브 다이어그램 렌더링
      ```

      <br>**분석 대상**
    + @RestController, @Service, @Repository 어노테이션 추적
    + 클래스 간 의존성(Injection) 관계 추출
    + API 엔드포인트(@RequestMapping) 매핑

<br>

## **🔸 페이지 구성**
<br>
  <table>
    <tr>
      <td align="center"><b>기술 블로그</b></td>
      <td align="center"><b>개인 노트</b></td>
    </tr>
    <tr>
      <td align="center"><img src="https://github.com/user-attachments/assets/5e2320f6-6e86-46b8-9a49-1ed207a5a5a4" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/fa836b5b-f068-438a-bc57-7f149278f133" /></td>
    </tr>
  </table>
  <table>
    <tr>
      <td align="center"><b>FlowMap</b></td>
      <td align="center"><b>Admin</b></td>
    </tr>
    <tr>
      <td align="center"><img src="https://github.com/user-attachments/assets/2e26027d-2c20-476f-bf10-4c8613afebe3" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/48ab46db-c560-4b39-bd9f-75fb584b5543" /></td>
    </tr>
  </table>
<br>
