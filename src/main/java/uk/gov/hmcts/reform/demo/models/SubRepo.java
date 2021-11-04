package uk.gov.hmcts.reform.demo.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubRepo extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByUuid(String uuid);

    List<Subscription> findAllByCaseID(String caseId);

    List<Subscription> findAllByCourtID(String courtId);

    List<Subscription> findAllByUrnID(String urnId);

    List<Subscription> findAllBySubscriptionID(String subId);

    @Override
    List<Subscription> findAll();

}

