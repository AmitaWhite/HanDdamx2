package com.white.handdam.subscription.repository;

import com.white.handdam.subscription.repository.projection.CreatorSubscriptionPlanProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SubscriptionPlanQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<CreatorSubscriptionPlanProjection> findByCreatorId(Long creatorId) {
        String sql = """
                SELECT member_id, subscription_price, benefits_description
                FROM creator_profile
                WHERE member_id = :creatorId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("creatorId", creatorId);
        List<CreatorSubscriptionPlanProjection> results = jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new CreatorSubscriptionPlanProjection(
                        rs.getLong("member_id"),
                        (Integer) rs.getObject("subscription_price"),
                        rs.getString("benefits_description")
                )
        );

        return results.stream().findFirst();
    }
}
