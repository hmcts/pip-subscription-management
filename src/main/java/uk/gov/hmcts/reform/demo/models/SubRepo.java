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


    List<Subscription> findAllById(Long subId); //finds all subs by subID

    @Override
    List<Subscription> findAll();

}

