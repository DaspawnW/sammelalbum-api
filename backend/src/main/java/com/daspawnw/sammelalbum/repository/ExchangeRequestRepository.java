package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
    List<ExchangeRequest> findByStatus(ExchangeStatus status);

    List<ExchangeRequest> findByRequesterId(Long requesterId);

    List<ExchangeRequest> findByOffererId(Long offererId);

    // FK-based queries for card deletion handling
    List<ExchangeRequest> findByOffererCardOfferId(Long cardOfferId);

    List<ExchangeRequest> findByRequesterCardSearchId(Long cardSearchId);

    List<ExchangeRequest> findByRequesterCardOfferId(Long cardOfferId);

    List<ExchangeRequest> findByOffererCardSearchId(Long cardSearchId);
}
