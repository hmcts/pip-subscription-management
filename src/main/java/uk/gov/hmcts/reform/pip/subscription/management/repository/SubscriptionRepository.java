package uk.gov.hmcts.reform.pip.subscription.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This JPA interface allows us to specify specific find methods for the database and it should
 * create the required functionality for us.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findById(UUID id);

    List<Subscription> findByUserId(String userId);

    @Override
    List<Subscription> findAll();

    void deleteById(UUID id);

    @Query(value = "SELECT * FROM Subscription WHERE search_type = :search_type AND search_value = :search_value",
        nativeQuery = true)
    List<Subscription> findSubscriptionsBySearchValue(@Param("search_type") String searchType,
                                                      @Param("search_value") String searchValue);

    void deleteAllByUserId(String userId);
}

