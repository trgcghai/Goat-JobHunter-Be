package iuh.fit.goat.dto.request.ticket;

import iuh.fit.goat.enumeration.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotNull
    private long targetId; // id of comment or blog

    @NotNull
    private ReportReason reason;

    private String description;

}
