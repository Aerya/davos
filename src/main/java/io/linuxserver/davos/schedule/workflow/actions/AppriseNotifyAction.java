package io.linuxserver.davos.schedule.workflow.actions;

import java.util.List;

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
 * API</a> server at the end of a schedule run. The stateless {@code /notify}
 * endpoint is used, so any service Apprise supports (Discord, Telegram, Gotify,
 * email, ...) can be reached. The list of downloaded files is split into chunks
 * (default 20 lines) sent as separate notifications, with a short pause between
 * them, to avoid downstream rate limits.
 */
public class AppriseNotifyAction implements ScheduleSummaryAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppriseNotifyAction.class);

    /** Number of file names per notification. */
    static final int BATCH_SIZE = 20;
    /** Pause between consecutive notifications, in milliseconds. */
    static final long DELAY_BETWEEN_MESSAGES_MS = 1000;

    private RestTemplate restTemplate = new RestTemplate();
    private String serverUrl;
    private String urls;
    private String scheduleName;

    public AppriseNotifyAction(String serverUrl, String urls, String scheduleName) {
        this.serverUrl = serverUrl;
        this.urls = urls;
        this.scheduleName = scheduleName;
    }

    @Override
    public void execute(List<String> downloadedFileNames) {

        if (null == downloadedFileNames || downloadedFileNames.isEmpty())
            return;

        int total = downloadedFileNames.size();
        String endpoint = StringUtils.removeEnd(StringUtils.trimToEmpty(serverUrl), "/") + "/notify";

        LOGGER.info("Sending Apprise digest for {} file(s) downloaded by schedule '{}' to {}", total, scheduleName, endpoint);

        for (int start = 0; start < total; start += BATCH_SIZE) {

            int end = Math.min(start + BATCH_SIZE, total);
            List<String> batch = downloadedFileNames.subList(start, end);

            String title = String.format("%s — %d file(s) downloaded", scheduleName, total);
            String body = String.format("Showing %d-%d of %d:\n%s", start + 1, end, total, String.join("\n", batch));

            sendMessage(endpoint, title, body);

            if (end < total)
                pause();
        }
    }

    private void sendMessage(String endpoint, String title, String messageBody) {

        AppriseRequest body = new AppriseRequest();
        body.urls = urls;
        body.title = title;
        body.body = messageBody;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {

            HttpEntity<AppriseRequest> httpEntity = new HttpEntity<AppriseRequest>(body, headers);
            restTemplate.exchange(endpoint, HttpMethod.POST, httpEntity, Object.class);

        } catch (RestClientException | HttpMessageConversionException e) {

            LOGGER.debug("Full stacktrace", e);
            LOGGER.error("Unable to complete notification to Apprise. Given error: {}", e.getMessage());
        }
    }

    private void pause() {

        try {
            Thread.sleep(DELAY_BETWEEN_MESSAGES_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
