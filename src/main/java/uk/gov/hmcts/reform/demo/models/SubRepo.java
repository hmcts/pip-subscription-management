package uk.gov.hmcts.reform.demo.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubRepo extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUuid(String uuid);

    @Override
    List<Subscription> findAll();

}

