package com.white.handdam.search.repository;

import com.white.handdam.category.entity.Category;
import com.white.handdam.category.repository.CategoryRepository;
import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.creator.repository.CreatorProfileRepository;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.project.entity.Project;
import com.white.handdam.project.repository.ProjectRepository;
import com.white.handdam.search.dto.response.CreatorSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색 리포지토리 JPQL 정합성 검증(H2). compileJava 로는 잡히지 않는 쿼리 오류를 실행으로 검증한다.
 */
@SpringBootTest(properties = {
        "jwt.secret=test_secret_for_search_repository_1234567890"
})
@ActiveProfiles("test")
@Transactional
class SearchRepositoryTest {

    @Autowired private FeedSearchRepository feedSearchRepository;
    @Autowired private CreatorSearchRepository creatorSearchRepository;

    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private FeedRepository feedRepository;
    @Autowired private CreatorProfileRepository creatorProfileRepository;

    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

    private static final PageRequest PAGE = PageRequest.of(0, 20);

    /** 크리에이터 + 카테고리 + 프로젝트 + PUBLIC 피드 한 세트를 저장하고 크리에이터 memberId 를 반환. */
    private long seed(String nickname, String feedTitle, String feedContent) {
        Member creator = Member.createLocalMember(nickname + "@test.com", "pw", nickname);
        creator.changeRoleToCreator();
        creator = memberRepository.save(creator);

        creatorProfileRepository.save(CreatorProfile.builder()
                .memberId(creator.getId())
                .introduction(nickname + " 소개글")
                .subscriptionPrice(0)
                .build());

        Category category = categoryRepository.save(Category.builder().name("카테고리-" + nickname).build());

        Project project = projectRepository.save(Project.builder()
                .creatorId(creator.getId())
                .categoryId(category.getId())
                .title("프로젝트-" + nickname)
                .build());

        feedRepository.save(Feed.create(project.getId(), feedTitle, feedContent, Visibility.PUBLIC));
        return creator.getId();
    }

    @Test
    @DisplayName("findSearchFeeds: 제목 keyword 로 PUBLIC 피드를 검색한다")
    void findSearchFeeds_matchesByTitle() {
        seed("민호공방", "손뜨개 목도리 만들기", "본문 내용");
        seed("서연", "가죽 지갑 클래스", "본문 내용");

        Slice<Feed> result = feedSearchRepository.findSearchFeeds("손뜨개", null, PAGE);

        assertThat(result.getContent()).extracting(Feed::getTitle).containsExactly("손뜨개 목도리 만들기");
    }

    @Test
    @DisplayName("findSearchFeeds: 본문 keyword 로도 검색되고, 매칭이 없으면 빈 결과")
    void findSearchFeeds_matchesByContentAndEmpty() {
        seed("민호공방", "목도리", "대바늘 뜨개질 팁");

        assertThat(feedSearchRepository.findSearchFeeds("대바늘", null, PAGE).getContent()).hasSize(1);
        assertThat(feedSearchRepository.findSearchFeeds("존재하지않는키워드", null, PAGE).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findSearchFeeds: keyword 가 null 이면 explore 처럼 전체 PUBLIC 피드를 반환")
    void findSearchFeeds_nullKeywordReturnsAll() {
        seed("민호공방", "손뜨개 목도리", "본문");
        seed("서연", "가죽 지갑", "본문");

        assertThat(feedSearchRepository.findSearchFeeds(null, null, PAGE).getContent()).hasSize(2);
    }

    @Test
    @DisplayName("searchCreators: 작가명(nickname) keyword 로 크리에이터를 검색한다")
    void searchCreators_matchesByNickname() {
        long minhoId = seed("민호공방", "손뜨개", "본문");
        seed("서연", "가죽 지갑", "본문");

        Slice<CreatorSearchResponse> result = creatorSearchRepository.searchCreators("민호", PAGE);

        assertThat(result.getContent()).extracting(CreatorSearchResponse::creatorId).containsExactly(minhoId);
        assertThat(result.getContent()).extracting(CreatorSearchResponse::nickname).containsExactly("민호공방");
    }
}
