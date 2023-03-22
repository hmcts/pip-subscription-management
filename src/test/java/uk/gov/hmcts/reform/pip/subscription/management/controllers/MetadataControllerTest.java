package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.service.MetadataService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataControllerTest {

    @Mock
    MetadataService metadataService;

    @InjectMocks
    MetadataController metadataController;

    @Test
    void retrieveChannels() {
        List<Channel> channelList = List.of(Channel.EMAIL);
        when(metadataService.retrieveChannels()).thenReturn(channelList);

        ResponseEntity<List<Channel>> channels = metadataController.retrieveChannels();

        assertEquals(channelList, channels.getBody(), "Unexpected list of channels returned");
        assertEquals(HttpStatus.OK, channels.getStatusCode(), "Unexpected status returned");
    }
}
