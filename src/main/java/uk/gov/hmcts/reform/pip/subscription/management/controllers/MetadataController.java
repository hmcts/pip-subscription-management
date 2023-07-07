package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.service.MetadataService;

import java.util.List;

/**
 * This controller returns any metadata for subscriptions, so that everything is in a single place.
 */
@RestController
@Tag(name = "Subscription Metadata API")
@RequestMapping("/meta")
@Valid
@IsAdmin
public class MetadataController {

    @Autowired
    MetadataService metadataService;

    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";
    private static final String AUTH_ERROR_CODE = "403";

    @GetMapping("/channels")
    @Operation(summary = "Endpoint to retrieve the available channels as a list")
    @ApiResponse(responseCode = "200", description = "List of channels returned in JSON array format")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    public ResponseEntity<List<Channel>> retrieveChannels() {
        return ResponseEntity.ok(metadataService.retrieveChannels());
    }


}
