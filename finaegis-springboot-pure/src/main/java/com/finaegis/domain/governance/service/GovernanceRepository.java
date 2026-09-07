package com.finaegis.domain.governance.service;

import com.finaegis.domain.governance.model.Poll;
import com.finaegis.domain.governance.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GovernanceRepository extends JpaRepository<Poll, String> {

    List<Poll> findByStatusAndEndDateAfter(Poll.PollStatus status, Instant now);
}
