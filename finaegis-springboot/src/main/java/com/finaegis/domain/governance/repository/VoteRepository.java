package com.finaegis.domain.governance.repository;

import com.finaegis.domain.governance.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, String> {

    Optional<Vote> findByPollIdAndUserId(String pollId, String userId);

    boolean existsByPollIdAndUserId(String pollId, String userId);

    long countByPollId(String pollId);

    List<Vote> findByPollId(String pollId);
}
