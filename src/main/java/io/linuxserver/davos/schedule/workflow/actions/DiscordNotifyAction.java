package io.linuxserver.davos.schedule.workflow.actions;

import java.util.List;

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
 * Sends a Discord webhook notification at the end of a schedule run, listing the
 * files that were downloaded. The list is split into chunks (default 20 lines)
 * sent as separate messages, with a short pause between them, so a large run
 * does not trip Discord's rate limiting.
 */
public class DiscordNotifyAction implements ScheduleSummaryAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordNotifyAction.class);

    /** Number of file names per Discord message. */
    static final int BATCH_SIZE = 20;
    /** Pause between consecutive messages, in milliseconds. */
    static final long DELAY_BETWEEN_MESSAGES_MS = 1000;

    private RestTemplate restTemplate = new RestTemplate();
    private String webhookUrl;
    private String scheduleName;

    public DiscordNotifyAction(String webhookUrl, String scheduleName) {
        this.webhookUrl = webhookUrl;
        this.scheduleName = scheduleName;
    }

    @Override
    public void execute(List<String> downloadedFileNames) {

        if (null == downloadedFileNames || downloadedFileNames.isEmpty())
            return;

        int total = downloadedFileNames.size();
        LOGGER.info("Sending Discord digest for {} file(s) downloaded by schedule '{}'", total, scheduleName);

        for (int start = 0; start < total; start += BATCH_SIZE) {

            int end = Math.min(start + BATCH_SIZE, total);
            List<String> batch = downloadedFileNames.subList(start, end);

            String header = String.format("**%s** — %d file(s) downloaded (showing %d-%d of %d):",
                    scheduleName, total, start + 1, end, total);
            String content = header + "\n" + String.join("\n", batch);

            sendMessage(content);

            if (end < total)
                pause();
        }
    }

    private void sendMessage(String content) {

        DiscordRequest body = new DiscordRequest();
        body.content = content;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {

            HttpEntity<DiscordRequest> httpEntity = new HttpEntity<DiscordRequest>(body, headers);
            restTemplate.exchange(webhookUrl, HttpMethod.POST, httpEntity, Object.class);

        } catch (RestClientException | HttpMessageConversionException e) {

            LOGGER.debug("Full stacktrace", e);
            LOGGER.error("Unable to complete notification to Discord. Given error: {}", e.getMessage());
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

    class DiscordRequest {

        public String content;

        @Override
        public String toString() {
            return "DiscordRequest [content=" + content + "]";
        }
    }
}
