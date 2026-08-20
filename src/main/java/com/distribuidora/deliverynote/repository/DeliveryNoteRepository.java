package com.distribuidora.deliverynote.repository;

import com.distribuidora.deliverynote.model.DeliveryNote;
import com.distribuidora.deliverynote.model.DeliveryNoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, UUID> {

    Page<DeliveryNote> findByOrderId(UUID orderId, Pageable pageable);

    Page<DeliveryNote> findByStatus(DeliveryNoteStatus status, Pageable pageable);

    Page<DeliveryNote> findByIssueDate(LocalDate issueDate, Pageable pageable);

    Page<DeliveryNote> findByDeliveryDate(LocalDate deliveryDate, Pageable pageable);

    List<DeliveryNote> findByDeliveryNoteNumber(String deliveryNoteNumber);

    @Query("SELECT MAX(d.deliveryNoteNumber) FROM DeliveryNote d WHERE d.deliveryNoteNumber LIKE CONCAT('R-', :year, '-%')")
    Optional<String> findMaxDeliveryNoteNumberForYear(int year);
}
