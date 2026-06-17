package io.linuxserver.davos.schedule.workflow.actions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Notifies through an <a href="https://github.com/caronc/apprise-api">Apprise
 * API</a> server. The stateless {@code /notify} endpoint is used: davos posts
 * the target Apprise URLs along with the message, so any service Apprise
 * supports (Discord, Telegram, Gotify, email, ...) can be reached.
 */
public class AppriseNotifyAction implements PostDownloadAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppriseNotifyAction.class);

    private RestTemplate restTemplate = new RestTemplate();
    private String serverUrl;
    private String urls;

    public AppriseNotifyAction(String serverUrl, String urls) {
        this.serverUrl = serverUrl;
        this.urls = urls;
    }

    @Override
    public void execute(PostDownloadExecution execution) {

        AppriseRequest body = new AppriseRequest();
        body.urls = urls;
        body.title = "A new file has been downloaded";
        body.body = execution.fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String endpoint = StringUtils.removeEnd(StringUtils.trimToEmpty(serverUrl), "/") + "/notify";

        try {

            LOGGER.info("Sending notification to Apprise for {}", execution.fileName);
            LOGGER.debug("Apprise endpoint: {}, urls: {}", endpoint, urls);
            HttpEntity<AppriseRequest> httpEntity = new HttpEntity<AppriseRequest>(body, headers);
            restTemplate.exchange(endpoint, HttpMethod.POST, httpEntity, Object.class);

        } catch (RestClientException | HttpMessageConversionException e) {

            LOGGER.debug("Full stacktrace", e);
            LOGGER.error("Unable to complete notification to Apprise. Given error: {}", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    class AppriseRequest {

        public String urls;
        public String title;
        public String body;

        @Override
        public String toString() {
            return "AppriseRequest [urls=" + urls + ", title=" + title + ", body=" + body + "]";
        }
    }
}
