package iuh.fit.goat.controller;


import iuh.fit.goat.dto.request.ticket.CreateTicketRequest;
import iuh.fit.goat.dto.response.ticket.TicketResponse;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import iuh.fit.goat.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/blog")
    public TicketResponse reportBlog(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createBlogTicket(request);
    }

    @PostMapping("/comment")
    public TicketResponse reportComment(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createCommentTicket(request);
    }

    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public List<TicketResponse> getTickets(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) TicketType type
    ) {
        return ticketService.getTickets(status, type);
    }
}