package io.linuxserver.davos.schedule.workflow.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import io.linuxserver.davos.schedule.workflow.actions.DiscordNotifyAction.DiscordRequest;

public class DiscordNotifyActionTest {

    @InjectMocks
    private DiscordNotifyAction discordNotifyAction;

    @Mock
    private RestTemplate mockRestTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<DiscordRequest>> entityCaptor;

    @Before
    public void setUp() {

        discordNotifyAction = new DiscordNotifyAction("https://discord.example/webhook", "Schedule");
        initMocks(this);
    }

    @Test
    public void executeShouldSendDigestWithDiscordCompatibleHeaders() {

        discordNotifyAction.execute(Arrays.asList("one.nzb", "two.nzb"));

        verify(mockRestTemplate).exchange(eq("https://discord.example/webhook"), eq(HttpMethod.POST),
                entityCaptor.capture(), eq(Object.class));

        HttpEntity<DiscordRequest> entity = entityCaptor.getValue();
        assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(entity.getHeaders().getFirst("User-Agent")).isEqualTo(DiscordNotifyAction.USER_AGENT);
        assertThat(entity.getBody().content).contains("**Schedule**", "one.nzb", "two.nzb");
    }

    @Test
    public void executeShouldNotSendAnythingWithoutDownloads() {

        discordNotifyAction.execute(Collections.<String>emptyList());

        verify(mockRestTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class),
                eq(Object.class));
    }

    @Test
    public void executeShouldSplitMessagesByLineCount() {

        List<String> files = new ArrayList<String>();
        for (int index = 0; index < 21; index++)
            files.add("file-" + index + ".nzb");

        discordNotifyAction.execute(files);

        verify(mockRestTemplate, times(2)).exchange(eq("https://discord.example/webhook"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Object.class));
    }

    @Test
    public void executeShouldKeepEveryMessageWithinDiscordLimit() {

        List<String> files = new ArrayList<String>();
        for (int index = 0; index < 20; index++)
            files.add(repeat("long-name-", 30) + index + ".nzb");

        discordNotifyAction.execute(files);

        verify(mockRestTemplate, times(4)).exchange(eq("https://discord.example/webhook"), eq(HttpMethod.POST),
                entityCaptor.capture(), eq(Object.class));

        for (HttpEntity<DiscordRequest> entity : entityCaptor.getAllValues())
            assertThat(entity.getBody().content.length()).isLessThanOrEqualTo(DiscordNotifyAction.MAX_CONTENT_LENGTH);
    }

    @Test
    public void executeShouldContinueWhenDiscordRejectsAMessage() {

        when(mockRestTemplate.exchange(eq("https://discord.example/webhook"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(Object.class))).thenThrow(new RestClientException("403 Forbidden"));

        discordNotifyAction.execute(Arrays.asList("one.nzb"));
    }

    private String repeat(String value, int count) {

        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++)
            result.append(value);

        return result.toString();
    }
}
