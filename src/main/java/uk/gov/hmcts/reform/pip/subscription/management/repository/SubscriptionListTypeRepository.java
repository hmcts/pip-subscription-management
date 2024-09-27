package uk.gov.hmcts.reform.pip.subscription.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionListTypeRepository extends JpaRepository<SubscriptionListType, Long> {

    Optional<SubscriptionListType> findByUserId(String userId);

    @Transactional
    void deleteByUserId(String userId);

    @Transactional
    void deleteByIdIn(List<UUID> id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE Subscription_List_Type "
        + "SET list_type = string_to_array(:list_type, ',') "
        + "WHERE user_id = :user_id",
        nativeQuery = true)
    void updateLocationSubscriptions(@Param("user_id") String userId,
                                     @Param("list_type") String listType);
}
