package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.subscription.management.service.ViewService;

/**
 * View controller which deals with the view for Account Management.
 */
@RestController
@Tag(name = "Subscription Management - API for dealing with views")
@RequestMapping("/view")
@IsAdmin
public class ViewController {

    @Autowired
    ViewService viewService;

    @ApiResponse(responseCode = "200", description = "View Refreshed")
    @ApiResponse(responseCode = "403", description = "User has not been authorised")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshView() {
        viewService.refreshView();
        return ResponseEntity.ok().build();
    }

}
