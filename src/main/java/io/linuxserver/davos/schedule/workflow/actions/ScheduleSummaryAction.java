package io.linuxserver.davos.schedule.workflow.actions;

import java.util.List;

/**
 * An action executed once at the end of a schedule run, receiving the names of
 * all files that were downloaded during that run. This is used for notifications
 * that should send a single digest (batched into chunks) rather than one message
 * per file, to avoid hitting downstream rate limits (e.g. Discord).
 */
public interface ScheduleSummaryAction {

    void execute(List<String> downloadedFileNames);
}
