package uk.gov.hmcts.reform.pip.subscription.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class MiReportLocal {
    private UUID id;
    private String searchValue;
    private Channel channel;
    private String userId;
    private String locationName;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdDate;
}

