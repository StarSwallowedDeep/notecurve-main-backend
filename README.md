<p align="center">
  <img src="https://github.com/user-attachments/assets/113caf7b-9c91-4fa4-86c8-b99d2964e8b0" />
</p>

# **NOTECURVE**
## **📑 개요**
<br>

+ **소개**
    + ### **" 기술 블로그 및 개인 노트 플랫폼 "**
      <br>**기술 경험과 인사이트를 공유**할 수 있는 블로그와, **개인 학습 내용을 기록**할 수 있는 노트를 제공하는 플랫폼입니다.  

      기술적 실험과 인터랙티브한 요소, 창의적 작업들이 모여 있는 다양한 프로젝트를 경험할 수 있는 공간을 제공합니다.  
<br>

+ **목적**
  
    + **체계적 기록**: 새로운 기술을 탐구하거나 프로젝트를 진행하면서 얻는 **다양한 경험과 배움을 한 곳**에 체계적으로  
    기록할 수 있도록 합니다.

    + **지식 통합**: 다른 사람들의 기술 글을 참고하며 자신의 학습 기록을 함께 관리할 수 있어, **개인 지식과 외부 인사이트를**  
    통합적으로 경험할 수 있는 환경을 제공합니다.

    + 사고 과정 · 기술적 통찰 · 구현 경험을 축적하여 필요할 때 다시 참고할 수 있는 **개인 지식 저장소**를 구축하고,  
    이를 통해 **기술 확장과 아이디어 탐색을 자연스럽게 이어갈 수 있는** 기술 아카이브 플랫폼을 만드는 것을 목표로 합니다.  
<br>

## **📌 주요 기능 및 페이지 구성**
<br>

+ **주요 기능**

    + **기술 블로그**   
      - 게시글 등록
      - 게시글 수정
      - 게시글 삭제
      - 게시글 조회
      - **MCP**: 게시글 요약 및 관련 글 정리

    + **개인 노트**  
      - 사용자 계정 기반 접근 제어
      - 노트 생성
      - 노트 수정
      - 노트 삭제
      - 노트 조회
      - **파일 첨부 기능**: 파일, 이미지, 문서 등 저장

    + **사용자 관리**  
      - 회원가입
      - 로그인 / 로그아웃 (JWT 기반 인증)
      - 비밀번호 변경
      - 이름 변경
      - 회원탈퇴
<br>

+ **페이지 구성**

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
      <td align="center"><b>사용자 관리</b></td>
      <td align="center"><b>게시글 작성</b></td>
    </tr>
    <tr>
      <td align="center"><img src="https://github.com/user-attachments/assets/3041e09e-450b-430c-ae65-eea57ef5f748" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/cbd32777-e38b-4336-81ec-3fd3be993e14" /></td>
    </tr>
  </table>
<br>

## **🔧 구현 과정**
<br>

1. **인증/인가 처리 방식 (Spring Security + JWT)**
    + JWT에서 사용자 정보(loginId)를 파싱
    + DB에서 해당 사용자를 조회하고 UserDetailsImpl 생성
    + SecurityContextHolder에 Authentication 저장
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

2. **최적 쿼리 적용**
    + 게시글(Post) 조회 기능에서 각 게시글은 작성자(User) 정보를 참조
    + 기본 상태(LAZY 로딩)에서는 게시글 10개를 조회할 경우, 각 게시글마다 User 정보를 추가로 조회하는 쿼리가 실행되는 문제 발생
    + 이를 해결하기 위해 JPQL의 JOIN FETCH를 이용하여 게시글과 작성자를 한 번의 쿼리로 함께 조회하도록 개선
      ```java
      @Query("SELECT p FROM Post p JOIN FETCH p.user")
      List<Post> findAllWithUser();

      @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :postId")
      Optional<Post> findByIdWithUser(@Param("postId") Long postId);
<br>

3. **트랜잭션 기반 서버 파일 관리**
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

## **🔸 아키텍처 구성**
<br>

+ **시스템 아키텍처**
    <p>
      <img width="1812" height="985" src="https://github.com/user-attachments/assets/d06fb7fa-a033-4ad0-9f0a-32f315c47603" style="width: 80%"/>
    </p>
<br>

+ **기술 스택**  

  + **Frontend**  
    ![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=white)
  + **Backend**  
    ![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)
    ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
  + **Database**  
    ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
  + **Cloud**  
    ![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)
<br>
