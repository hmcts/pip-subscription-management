package uk.gov.hmcts.reform.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.demo.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.demo.repository.SubRepo;
import uk.gov.hmcts.reform.demo.models.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for dealing with subscriptions.
 */
@Component
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    SubRepo repository;

    @Override
    public Subscription createSubscription(Subscription subscription) {
        return repository.save(subscription);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public void deleteById(Long id) {
        List<Subscription> subscriptionList = repository.findAllById(id);

        if (subscriptionList.isEmpty()) {
         throw new SubscriptionNotFoundException("No subscription with that id exists");
        }
    }

    @Override
    public List<Subscription> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Subscription> findById(Long subscriptionId) {
        return repository.findById(subscriptionId);
    }
}
