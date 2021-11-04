package uk.gov.hmcts.reform.demo.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubRepo extends JpaRepository<Subscription, Long> {
    /**
     * This JPA interface allows us to specify specific find methods for the database and it should
     * create the required functionality for us.
     */
    List<Subscription> findAllByUuid(String uuid); //finds all subs associated with a user

    List<Subscription> findAllByCaseId(String caseId); //finds all subs associated with a caseID

    List<Subscription> findAllByCourtId(String courtId); //finds all subs by courtID

    List<Subscription> findAllByUrnId(String urnId); //finds all subs by urnID

    List<Subscription> findAllBySubscriptionId(String subId); //finds all subs by subID

    @Override
    List<Subscription> findAll();

}

