package com.distribuidora.repository;

import com.distribuidora.model.DeliveryWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryWindowRepository extends JpaRepository<DeliveryWindow, UUID> {

    List<DeliveryWindow> findAllByOrderByCutoffDayOfWeekAscCutoffTimeAsc();

    List<DeliveryWindow> findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc();
}
