package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.ticket.CreateTicketRequest;
import iuh.fit.goat.dto.response.ticket.TicketResponse;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import java.util.List;

public interface TicketService {
    TicketResponse createBlogTicket(CreateTicketRequest request);
    TicketResponse createCommentTicket(CreateTicketRequest request);
    List<TicketResponse> getTickets(
            Status status,
            TicketType type
    );
}
