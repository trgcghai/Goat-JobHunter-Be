package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.ticket.CreateTicketRequest;
import iuh.fit.goat.dto.response.ticket.TicketResponse;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import iuh.fit.goat.exception.InvalidException;

import java.util.List;

public interface TicketService {
    TicketResponse createBlogTicket(CreateTicketRequest request) throws InvalidException;
    TicketResponse createCommentTicket(CreateTicketRequest request) throws InvalidException;
    List<TicketResponse> getTickets(
            Status status,
            TicketType type
    );
}
