package com.finaegis.presentation.rest;

import com.finaegis.domain.governance.model.Poll;
import com.finaegis.domain.governance.model.Vote;
import com.finaegis.domain.governance.repository.PollRepository;
import com.finaegis.domain.governance.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;

    @GetMapping("/polls")
    public ResponseEntity<List<Poll>> listActivePolls() {
        return ResponseEntity.ok(pollRepository
            .findByStatusAndEndDateAfter(Poll.PollStatus.ACTIVE, Instant.now()));
    }

    @GetMapping("/polls/all")
    public ResponseEntity<List<Poll>> listAll() {
        return ResponseEntity.ok(pollRepository.findAll());
    }

    @PostMapping("/polls")
    public ResponseEntity<Poll> createPoll(@RequestBody CreatePollRequest request) {
        Poll poll = Poll.builder()
            .id(UUID.randomUUID().toString())
            .title(request.title())
            .description(request.description())
            .type(Poll.PollType.valueOf(request.type()))
            .status(Poll.PollStatus.DRAFT)
            .createdBy(request.createdBy())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollRepository.save(poll));
    }

    @PostMapping("/polls/{pollId}/activate")
    public ResponseEntity<Void> activate(@PathVariable String pollId) {
        pollRepository.findById(pollId).ifPresent(poll -> {
            poll.setStatus(Poll.PollStatus.ACTIVE);
            pollRepository.save(poll);
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/polls/{pollId}/vote")
    public ResponseEntity<Void> vote(@PathVariable String pollId, @RequestBody VoteRequest request) {
        if (voteRepository.existsByPollIdAndUserId(pollId, request.userId())) {
            throw new RuntimeException("User already voted in this poll");
        }
        Vote vote = Vote.builder()
            .id(UUID.randomUUID().toString())
            .pollId(pollId)
            .userId(request.userId())
            .optionKey(request.option())
            .votingPower(request.votingPower() != null ? request.votingPower() : BigDecimal.ONE)
            .build();
        voteRepository.save(vote);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/polls/{pollId}/results")
    public ResponseEntity<Map<String, Long>> results(@PathVariable String pollId) {
        Map<String, Long> counts = voteRepository.findByPollId(pollId).stream()
            .collect(Collectors.groupingBy(Vote::getOptionKey, Collectors.counting()));
        return ResponseEntity.ok(counts);
    }

    public record CreatePollRequest(String title, String description, String type,
                                    String createdBy, Instant startDate, Instant endDate) {}
    public record VoteRequest(String userId, String option, BigDecimal votingPower) {}
}
