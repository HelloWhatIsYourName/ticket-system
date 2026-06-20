package com.example.aiticket.ticket.web;

import com.example.aiticket.common.api.ApiResponse;
import com.example.aiticket.security.AuthenticatedUser;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.service.CreateTicketFromAiSessionCommand;
import com.example.aiticket.ticket.service.TicketWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketWorkflowService service;

    public TicketController(TicketWorkflowService service) {
        this.service = service;
    }

    @PostMapping("/from-ai-session")
    @PreAuthorize("hasAuthority('ticket:create')")
    public ApiResponse<TicketResponse> createFromAiSession(@Valid @RequestBody CreateTicketFromAiSessionRequest request,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        Ticket ticket = service.createFromAiSession(user.id(), primaryRole(user), new CreateTicketFromAiSessionCommand(
                request.sessionId(),
                request.assistantMessageId(),
                request.title(),
                request.description(),
                request.categoryId(),
                request.priority(),
                request.transferReason()
        ));
        return ApiResponse.ok(TicketResponse.from(ticket));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ticket:view:own')")
    public ApiResponse<List<TicketResponse>> myTickets(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.listCreatedTickets(user.id(), 100).stream()
                .map(TicketResponse::from)
                .toList());
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAuthority('ticket:process')")
    public ApiResponse<List<TicketResponse>> assignedTickets(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.listAssignedTickets(user.id(), 100).stream()
                .map(TicketResponse::from)
                .toList());
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('ticket:manage')")
    public ApiResponse<List<TicketResponse>> managedTickets() {
        return ApiResponse.ok(service.listManagedTickets(100).stream()
                .map(TicketResponse::from)
                .toList());
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasAnyAuthority('ticket:view:own','ticket:process','ticket:manage')")
    public ApiResponse<TicketDetailResponse> detail(@PathVariable Long ticketId,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketDetailResponse.from(service.getTicketDetail(
                user.id(),
                ticketId,
                user.permissions().contains("ticket:manage"),
                user.permissions().contains("ticket:process")
        )));
    }

    @PostMapping("/{ticketId}/comments")
    @PreAuthorize("hasAnyAuthority('ticket:view:own','ticket:process','ticket:manage')")
    public ApiResponse<TicketCommentResponse> addComment(@PathVariable Long ticketId,
                                                         @Valid @RequestBody CreateTicketCommentRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketCommentResponse.from(service.addComment(
                user.id(),
                primaryRole(user),
                ticketId,
                user.permissions().contains("ticket:manage"),
                user.permissions().contains("ticket:process"),
                request.toCommand()
        )));
    }

    @GetMapping("/{ticketId}/comments")
    @PreAuthorize("hasAnyAuthority('ticket:view:own','ticket:process','ticket:manage')")
    public ApiResponse<List<TicketCommentResponse>> comments(@PathVariable Long ticketId,
                                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.listComments(
                        user.id(),
                        ticketId,
                        user.permissions().contains("ticket:manage"),
                        user.permissions().contains("ticket:process")
                ).stream()
                .map(TicketCommentResponse::from)
                .toList());
    }

    @PostMapping("/{ticketId}/assign")
    @PreAuthorize("hasAuthority('ticket:assign')")
    public ApiResponse<TicketResponse> assign(@PathVariable Long ticketId,
                                              @Valid @RequestBody AssignTicketRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketResponse.from(service.assign(
                user.id(), primaryRole(user), ticketId, request.assigneeId(), request.comment())));
    }

    @PostMapping("/{ticketId}/start")
    @PreAuthorize("hasAuthority('ticket:process')")
    public ApiResponse<TicketResponse> start(@PathVariable Long ticketId,
                                             @Valid @RequestBody TicketActionRequest request,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketResponse.from(service.startProcessing(
                user.id(), primaryRole(user), ticketId, request.comment())));
    }

    @PostMapping("/{ticketId}/resolve")
    @PreAuthorize("hasAuthority('ticket:process')")
    public ApiResponse<TicketResponse> resolve(@PathVariable Long ticketId,
                                               @Valid @RequestBody TicketActionRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketResponse.from(service.resolve(
                user.id(), primaryRole(user), ticketId, request.comment())));
    }

    @PostMapping("/{ticketId}/reopen")
    @PreAuthorize("hasAuthority('ticket:view:own')")
    public ApiResponse<TicketResponse> reopen(@PathVariable Long ticketId,
                                              @Valid @RequestBody TicketActionRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketResponse.from(service.reopen(
                user.id(), primaryRole(user), ticketId, request.comment())));
    }

    @PostMapping("/{ticketId}/confirm-close")
    @PreAuthorize("hasAuthority('ticket:view:own')")
    public ApiResponse<TicketResponse> confirmClose(@PathVariable Long ticketId,
                                                    @Valid @RequestBody TicketActionRequest request,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketResponse.from(service.confirmClose(
                user.id(), primaryRole(user), ticketId, request.comment())));
    }

    @PostMapping("/{ticketId}/close")
    @PreAuthorize("hasAuthority('ticket:manage')")
    public ApiResponse<TicketResponse> close(@PathVariable Long ticketId,
                                             @Valid @RequestBody TicketActionRequest request,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(TicketResponse.from(service.close(
                user.id(), primaryRole(user), ticketId, request.comment())));
    }

    private String primaryRole(AuthenticatedUser user) {
        if (user.roles() == null || user.roles().isEmpty()) {
            return null;
        }
        return user.roles().getFirst();
    }
}
