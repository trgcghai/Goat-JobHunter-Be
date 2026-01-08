package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Ticket;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.enumeration.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    List<Ticket> findByStatus(Status status);

    List<Ticket> findByType(TicketType type);

    List<Ticket> findByStatusAndType(Status status, TicketType type);
}
