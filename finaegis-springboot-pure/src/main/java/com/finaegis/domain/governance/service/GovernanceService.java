package com.finaegis.domain.governance.service;

import com.finaegis.domain.governance.model.Poll;
import com.finaegis.domain.governance.model.Vote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GovernanceService {

    private final GovernanceRepository governanceRepository;
    private final VoteRepository voteRepository;

    public Poll createPoll(String title, String description, Poll.PollType type,
                           String createdBy, Instant endDate) {
        Poll poll = Poll.builder()
            .id(UUID.randomUUID().toString())
            .title(title)
            .description(description)
            .type(type)
            .status(Poll.PollStatus.ACTIVE)
            .createdBy(createdBy)
            .endDate(endDate)
            .build();
        return governanceRepository.save(poll);
    }

    public List<Poll> activePolls() {
        return governanceRepository.findByStatusAndEndDateAfter(Poll.PollStatus.ACTIVE, Instant.now());
    }

    public List<Poll> allPolls() {
        return governanceRepository.findAll();
    }

    public void vote(String pollId, String userId, String option, BigDecimal votingPower) {
        if (voteRepository.existsByPollIdAndUserId(pollId, userId)) {
            throw new IllegalStateException("User already voted in this poll");
        }
        Vote vote = Vote.builder()
            .id(UUID.randomUUID().toString())
            .pollId(pollId)
            .userId(userId)
            .optionKey(option)
            .votingPower(votingPower != null ? votingPower : BigDecimal.ONE)
            .build();
        voteRepository.save(vote);
    }

    public Map<String, Long> results(String pollId) {
        return voteRepository.findByPollId(pollId).stream()
            .collect(Collectors.groupingBy(Vote::getOptionKey, Collectors.counting()));
    }
}
