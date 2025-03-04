package uk.gov.hmcts.reform.pip.subscription.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This JPA interface allows us to specify specific find methods for the database and it should
 * create the required functionality for us.
 */
@Repository
@SuppressWarnings({"PMD.TooManyMethods"})
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findById(UUID id);

    List<Subscription> findByIdIn(List<UUID> id);

    List<Subscription> findByUserId(String userId);

    void deleteById(UUID id);

    void deleteByIdIn(List<UUID> id);

    @Query(value = "SELECT * FROM Subscription "
        + "WHERE search_type = :search_type "
        + "AND search_value = :search_value "
        + "AND :search_type <> 'LOCATION_ID'",
        nativeQuery = true)
    List<Subscription> findSubscriptionsBySearchValue(@Param("search_type") String searchType,
                                                      @Param("search_value") String searchValue);

    @Query("SELECT new uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData("
        + "id, channel, searchType, userId, locationName, createdDate) "
        + "FROM Subscription")
    List<AllSubscriptionMiData> getAllSubsDataForMiV2();

    @Query("SELECT new uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData("
        + "s.id, s.searchValue, s.channel, s.userId, s.locationName, s.createdDate) "
        + "FROM Subscription s WHERE s.searchType ='LOCATION_ID'")
    List<LocationSubscriptionMiData> getLocationSubsDataForMiV2();

    @Query(value = "SELECT s.* FROM Subscription s "
        + "INNER JOIN Subscription_List_Type sl "
        + "ON s.user_id = sl.user_id "
        + "WHERE s.search_type = 'LOCATION_ID' "
        + "AND s.search_value = :search_value "
        + "AND (ARRAY_LENGTH(sl.list_type, 1) IS NULL OR  sl.list_type && string_to_array(:list_type, ',')) "
        + "AND sl.list_language && string_to_array(:list_language, ',')",
        nativeQuery = true)
    List<Subscription> findSubscriptionsByLocationSearchValue(@Param("search_value") String searchValue,
                                                              @Param("list_type") String listType,
                                                              @Param("list_language") String listLanguage);

    void deleteAllByUserId(String userId);

    @Query(value = "SELECT * FROM Subscription "
        + "WHERE search_value = :search_value "
        + "AND search_type = 'LOCATION_ID'",
        nativeQuery = true)
    List<Subscription> findSubscriptionsByLocationId(@Param("search_value") String searchValue);

    @Query(value = "SELECT * FROM Subscription "
        + "WHERE user_id = :user_id "
        + "AND search_type = 'LOCATION_ID'",
        nativeQuery = true)
    List<Subscription> findLocationSubscriptionsByUserId(@Param("user_id") String userId);

    List<Subscription> findAllByLocationNameStartingWithIgnoreCase(@Param("prefix") String prefix);

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW sdp_mat_view_subscription", nativeQuery = true)
    void refreshSubscriptionView();
}
