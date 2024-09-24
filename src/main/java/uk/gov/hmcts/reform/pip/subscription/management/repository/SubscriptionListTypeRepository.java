package uk.gov.hmcts.reform.pip.subscription.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;

import java.util.List;

@Repository
public interface SubscriptionListTypeRepository extends JpaRepository<SubscriptionListType, Long> {

    List<SubscriptionListType> findByUserId(String userId);

    SubscriptionListType findSubscriptionListTypeByLocationIdAndUserId(Integer locationId, String userId);
}
