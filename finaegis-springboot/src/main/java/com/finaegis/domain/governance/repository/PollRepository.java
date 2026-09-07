package com.finaegis.domain.governance.repository;

import com.finaegis.domain.governance.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, String> {
    List<Poll> findByStatusOrderByCreatedAtDesc(Poll.PollStatus status);
    List<Poll> findByStatusAndEndDateAfter(Poll.PollStatus status, java.time.Instant endDate);
}
