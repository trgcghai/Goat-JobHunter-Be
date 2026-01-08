package iuh.fit.goat.dto.response.ticket;


import iuh.fit.goat.enumeration.ReportReason;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long ticketId;
    private TicketType type;
    private ReportReason reason;
    private String description;
    private Status status;
    private Long reporterId;
    private String reporterName;
    private Long blogId;
    private Long commentId;

}
