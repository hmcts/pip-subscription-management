package uk.gov.hmcts.reform.pip.subscription.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionListTypeRepository extends JpaRepository<SubscriptionListType, Long> {

    List<SubscriptionListType> findByUserId(String userId);

    void deleteAllByUserId(String userId);

    List<SubscriptionListType> findSubscriptionListTypeByLocationId(Integer locationId);

    void deleteByIdIn(List<UUID> id);

    Optional<SubscriptionListType> findSubscriptionListTypeByLocationIdAndUserId(Integer locationId, String userId);
}
