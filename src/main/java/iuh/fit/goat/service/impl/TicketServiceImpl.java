package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.ticket.CreateTicketRequest;
import iuh.fit.goat.dto.response.ticket.TicketResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.TicketService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final AccountRepository accountRepository;

    @Override
    public TicketResponse createBlogTicket(CreateTicketRequest request) throws InvalidException {
        Account reporter = getCurrentAccount();
        Blog blog = this.blogRepository.findById(request.getTargetId())
                .orElseThrow(() -> new RuntimeException("Blog is not exist"));

        if(blog.getAuthor().getAccountId() == (reporter.getAccountId())) {
            throw new InvalidException("You cannot report your own blog");
        }

        Ticket ticket = new Ticket();
        ticket.setType(TicketType.BLOG_REPORT);
        ticket.setReason(request.getReason());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(Status.PENDING);
        ticket.setReporter(reporter);
        ticket.setBlog(blog);

        Ticket savedTicket = this.ticketRepository.save(ticket);
        return mapToResponse(savedTicket);

    }

    @Override
    public TicketResponse createCommentTicket(CreateTicketRequest request) throws InvalidException {
        Account reporter = getCurrentAccount();

        Comment comment = this.commentRepository.findById(request.getTargetId())
                .orElseThrow(() -> new InvalidException("Comment is not exist"));

        if(comment.getCommentedBy().getAccountId() == reporter.getAccountId()) {
            throw new InvalidException("You cannot report your own blog");
        }

        Ticket ticket = new Ticket();
        ticket.setType(TicketType.COMMENT_REPORT);
        ticket.setReason(request.getReason());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(Status.PENDING);
        ticket.setReporter(reporter);
        ticket.setComment(comment);

        Ticket savedTicket = this.ticketRepository.save(ticket);
        return mapToResponse(savedTicket);
    }

    @Override
    public List<TicketResponse> getTickets(Status status, TicketType type) {
        List<Ticket> tickets;

        if (status != null && type != null) {
            tickets = this.ticketRepository.findByStatusAndType(status, type);
        } else if (status != null) {
            tickets = this.ticketRepository.findByStatus(status);
        } else if (type != null) {
            tickets = this.ticketRepository.findByType(type);
        } else {
            tickets = this.ticketRepository.findAll();
        }

        return tickets.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Account getCurrentAccount() throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        if (email.isBlank()) {
            throw new InvalidException("undefined user email");
        }

        return this.accountRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new InvalidException("User is not exist"));
    }


    private TicketResponse mapToResponse(Ticket ticket) {
        Account reporter = ticket.getReporter();
        TicketResponse response = new TicketResponse();
        response.setTicketId(ticket.getTicketId());
        response.setType(ticket.getType());
        response.setReason(ticket.getReason());
        response.setDescription(ticket.getDescription());
        response.setStatus(ticket.getStatus());
        response.setReporterId(reporter != null ? reporter.getAccountId() : null);
        response.setReporterName(reporter != null ? reporter.getUsername() : null);
        response.setBlogId(ticket.getBlog() != null ? ticket.getBlog().getBlogId() : null);
        response.setCommentId(ticket.getComment() != null ? ticket.getComment().getCommentId() : null);
        return response;
    }


}
