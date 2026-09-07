package com.finaegis.presentation.rest;

import com.finaegis.domain.governance.model.Poll;
import com.finaegis.domain.governance.service.GovernanceService;
import com.finaegis.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceService governanceService;

    @GetMapping("/polls")
    @PreAuthorize("hasAuthority('" + PermissionConstants.GOVERNANCE_POLL_VIEW + "')")
    public ResponseEntity<List<Poll>> listActivePolls() {
        return ResponseEntity.ok(governanceService.activePolls());
    }

    @GetMapping("/polls/all")
    @PreAuthorize("hasAuthority('" + PermissionConstants.GOVERNANCE_POLL_VIEW + "')")
    public ResponseEntity<List<Poll>> listAll() {
        return ResponseEntity.ok(governanceService.allPolls());
    }

    @PostMapping("/polls")
    @PreAuthorize("hasAuthority('" + PermissionConstants.GOVERNANCE_POLL_CREATE + "')")
    public ResponseEntity<Poll> createPoll(@RequestBody CreatePollRequest request) {
        Poll.PollType type = "YES_NO".equalsIgnoreCase(request.type())
            ? Poll.PollType.YES_NO
            : Poll.PollType.valueOf(request.type().toUpperCase());
        Poll poll = governanceService.createPoll(
            request.title(),
            request.description(),
            type,
            request.createdBy(),
            request.endDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(poll);
    }

    @PostMapping("/polls/{pollId}/vote")
    @PreAuthorize("hasAuthority('" + PermissionConstants.GOVERNANCE_VOTE + "')")
    public ResponseEntity<Void> vote(@PathVariable String pollId, @RequestBody VoteRequest request) {
        governanceService.vote(pollId, request.userId(), request.option(), request.votingPower());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/polls/{pollId}/results")
    @PreAuthorize("hasAuthority('" + PermissionConstants.GOVERNANCE_POLL_VIEW + "')")
    public ResponseEntity<Map<String, Long>> results(@PathVariable String pollId) {
        return ResponseEntity.ok(governanceService.results(pollId));
    }

    public record CreatePollRequest(String title, String description, String type,
                                    String createdBy, Instant endDate) {}
    public record VoteRequest(String userId, String option, BigDecimal votingPower) {}
}
