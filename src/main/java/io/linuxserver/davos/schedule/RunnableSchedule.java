package io.linuxserver.davos.schedule;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.linuxserver.davos.persistence.dao.ScheduleDAO;
import io.linuxserver.davos.persistence.model.ScheduleModel;
import io.linuxserver.davos.schedule.workflow.ScheduleWorkflow;
import io.linuxserver.davos.schedule.workflow.transfer.FTPTransfer;

public class RunnableSchedule implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnableSchedule.class);
    
    private ScheduleDAO configurationDAO;
    private Long scheduleId;
    private ScheduleWorkflow scheduleWorkflow;

    public RunnableSchedule(Long scheduleId, ScheduleDAO configurationDAO) {

        this.scheduleId = scheduleId;
        this.configurationDAO = configurationDAO;
    }

    @Override
    public void run() {

        LOGGER.info("Starting schedule {}", scheduleId);

        ScheduleModel model = configurationDAO.fetchSchedule(scheduleId);

        // Tag this run's log events with the schedule name so they can be routed
        // to a dedicated per-schedule log file (see log4j2 configuration).
        MDC.put("scheduleName", toLogFileName(model.name, scheduleId));

        try {

            ScheduleConfiguration config = ScheduleConfigurationFactory.createConfig(model);
            scheduleWorkflow = new ScheduleWorkflow(config);

            LOGGER.debug("Setting last scanned files on workflow before starting.");
            scheduleWorkflow.getFilesFromLastScan().addAll(model.scannedFiles.stream().map(sf -> sf.file).collect(toList()));

            LOGGER.debug("Starting workflow");
            scheduleWorkflow.start();
            LOGGER.debug("Workflow finished");

            LOGGER.debug("Saving newly scanned files against schedule");
            configurationDAO.updateScannedFilesOnSchedule(scheduleId, scheduleWorkflow.getFilesFromLastScan());

        } finally {
            MDC.remove("scheduleName");
        }
    }

    private static String toLogFileName(String scheduleName, Long scheduleId) {

        String sanitised = StringUtils.trimToEmpty(scheduleName).replaceAll("[^a-zA-Z0-9-_. ]", "_").trim();

        if (StringUtils.isBlank(sanitised))
            return "schedule-" + scheduleId;

        return sanitised;
    }
    
    public List<FTPTransfer> getTransfers() {
        return scheduleWorkflow.getFilesToDownload();
    }
}
