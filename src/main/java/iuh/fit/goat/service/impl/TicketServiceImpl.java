package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.ticket.CreateTicketRequest;
import iuh.fit.goat.dto.response.ticket.TicketResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Ticket;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.TicketRepository;
import iuh.fit.goat.repository.UserRepository;
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
    private final UserRepository userRepository;


    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        if (email.isBlank()) {
            throw new RuntimeException("undefined user email");
        }

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User is not exist");
        }

        return user;
    }

    @Override
    public TicketResponse createBlogTicket(CreateTicketRequest request) throws PermissionException {
        User reporter = getCurrentUser();
        Blog blog = blogRepository.findById(request.getTargetId())
                .orElseThrow(() -> new RuntimeException("Blog is not exist"));

        if(blog.getAuthor().getAccountId() == (reporter.getAccountId())) {
            throw new PermissionException("You cannot report your own blog");
        }

        Ticket ticket = new Ticket();
        ticket.setType(TicketType.BLOG_REPORT);
        ticket.setReason(request.getReason());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(Status.PENDING);
        ticket.setReporter(reporter);
        ticket.setBlog(blog);

        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToResponse(savedTicket);

    }

    @Override
    public TicketResponse createCommentTicket(CreateTicketRequest request) throws PermissionException {
        User reporter = getCurrentUser();

        Comment comment = commentRepository.findById(request.getTargetId())
                .orElseThrow(() -> new RuntimeException("Comment is not exist"));

        if(comment.getCommentedBy().getAccountId() == reporter.getAccountId()) {
            throw new PermissionException("You cannot report your own blog");
        }

        Ticket ticket = new Ticket();
        ticket.setType(TicketType.COMMENT_REPORT);
        ticket.setReason(request.getReason());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(Status.PENDING);
        ticket.setReporter(reporter);
        ticket.setComment(comment);

        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToResponse(savedTicket);
    }

    @Override
    public List<TicketResponse> getTickets(Status status, TicketType type) {
        List<Ticket> tickets;

        if (status != null && type != null) {
            tickets = ticketRepository.findByStatusAndType(status, type);
        } else if (status != null) {
            tickets = ticketRepository.findByStatus(status);
        } else if (type != null) {
            tickets = ticketRepository.findByType(type);
        } else {
            tickets = ticketRepository.findAll();
        }

        return tickets.stream()
                .map(this::mapToResponse)
                .toList();
    }


    private TicketResponse mapToResponse(Ticket ticket) {
        User reporter = ticket.getReporter();
        TicketResponse response = new TicketResponse();
        response.setTicketId(ticket.getTicketId());
        response.setType(ticket.getType());
        response.setReason(ticket.getReason());
        response.setDescription(ticket.getDescription());
        response.setStatus(ticket.getStatus());
        response.setReporterId(reporter != null ? reporter.getAccountId() : null);
        response.setReporterName(reporter != null ? reporter.getFullName() : null);
        response.setBlogId(ticket.getBlog() != null ? ticket.getBlog().getBlogId() : null);
        response.setCommentId(ticket.getComment() != null ? ticket.getComment().getCommentId() : null);
        return response;
    }


}
