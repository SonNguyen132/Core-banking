package com.finaegis.domain.governance.service;

import com.finaegis.domain.governance.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, String> {

    boolean existsByPollIdAndUserId(String pollId, String userId);

    List<Vote> findByPollId(String pollId);
}
