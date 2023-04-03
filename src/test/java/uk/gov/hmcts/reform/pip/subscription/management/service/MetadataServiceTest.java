package uk.gov.hmcts.reform.pip.subscription.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

    @InjectMocks
    MetadataService metadataService;

    @Test
    void retrieveChannels() {
        List<Channel> channels = metadataService.retrieveChannels();
        assertEquals(List.of(Channel.values()), channels, "Unexpected channels have been returned");
    }

}
