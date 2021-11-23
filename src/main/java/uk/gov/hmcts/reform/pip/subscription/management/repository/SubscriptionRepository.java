package uk.gov.hmcts.reform.pip.subscription.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    /**
     * This JPA interface allows us to specify specific find methods for the database and it should
     * create the required functionality for us.
     */

    List<Subscription> findAllById(Long subId); //finds all subs by subID



    @Override
    List<Subscription> findAll();

}

