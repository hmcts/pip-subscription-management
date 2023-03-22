package uk.gov.hmcts.reform.pip.subscription.management.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.util.List;

/**
 * Service layer which handles the retrieval of metadata associated with subscriptions.
 */
@Service
public class MetadataService {

    /**
     * Returns a list of channels associated with the subscription.
     * @return The list of channels.
     */
    public List<Channel> retrieveChannels() {
        return List.of(Channel.values());
    }

}
