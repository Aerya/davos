package io.linuxserver.davos.schedule.workflow.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class DiscordNotifyAction implements PostDownloadAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordNotifyAction.class);

    private RestTemplate restTemplate = new RestTemplate();
    private String webhookUrl;

    public DiscordNotifyAction(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void execute(PostDownloadExecution execution) {

        DiscordRequest body = new DiscordRequest();
        body.content = "A new file has been downloaded: " + execution.fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {

            LOGGER.info("Sending notification to Discord for {}", execution.fileName);
            LOGGER.debug("Webhook URL: {}", webhookUrl);
            HttpEntity<DiscordRequest> httpEntity = new HttpEntity<DiscordRequest>(body, headers);
            restTemplate.exchange(webhookUrl, HttpMethod.POST, httpEntity, Object.class);

        } catch (RestClientException | HttpMessageConversionException e) {

            LOGGER.debug("Full stacktrace", e);
            LOGGER.error("Unable to complete notification to Discord. Given error: {}", e.getMessage());
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
