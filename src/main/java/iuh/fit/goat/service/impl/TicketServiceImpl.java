package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.ticket.CreateTicketRequest;
import iuh.fit.goat.dto.response.ticket.TicketResponse;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Comment;
import iuh.fit.goat.entity.Ticket;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import iuh.fit.goat.repository.BlogRepository;
import iuh.fit.goat.repository.CommentRepository;
import iuh.fit.goat.repository.TicketRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.TicketService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public TicketResponse createBlogTicket(CreateTicketRequest request) {
        User reporter = getCurrentUser();
        Blog blog = blogRepository.findById(request.getTargetId())
                .orElseThrow(() -> new RuntimeException("Blog is not exist"));

        Ticket ticket = Ticket.builder()
                .type(TicketType.BLOG_REPORT)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(Status.PENDING)
                .reporter(reporter)
                .blog(blog)
                .build();

        return mapToResponse(ticketRepository.save(ticket));

    }

    @Override
    public TicketResponse createCommentTicket(CreateTicketRequest request) {
        User reporter = getCurrentUser();

        Comment comment = commentRepository.findById(request.getTargetId())
                .orElseThrow(() -> new RuntimeException("Comment is not exist"));

        Ticket ticket = Ticket.builder()
                .type(TicketType.COMMENT_REPORT)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(Status.PENDING)
                .reporter(reporter)
                .comment(comment)
                .build();

        return mapToResponse(ticketRepository.save(ticket));
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
        return TicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .type(ticket.getType())
                .reason(ticket.getReason())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .reporterId( reporter != null ? reporter.getAccountId() : null)
                .reporterName(reporter != null ? reporter.getFullName() : null)
                .blogId(ticket.getBlog() != null ? ticket.getBlog().getBlogId() : null)
                .commentId(ticket.getComment() != null ? ticket.getComment().getCommentId() : null)
                .build();
    }


}
