package io.linuxserver.davos.delegation.services;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.linuxserver.davos.converters.HostConverter;
import io.linuxserver.davos.exception.HostInUseException;
import io.linuxserver.davos.persistence.dao.HostDAO;
import io.linuxserver.davos.persistence.dao.ScheduleDAO;
import io.linuxserver.davos.persistence.model.HostModel;
import io.linuxserver.davos.transfer.ftp.FTPFile;
import io.linuxserver.davos.transfer.ftp.client.Client;
import io.linuxserver.davos.transfer.ftp.client.ClientFactory;
import io.linuxserver.davos.transfer.ftp.client.UserCredentials;
import io.linuxserver.davos.transfer.ftp.client.UserCredentials.Identity;
import io.linuxserver.davos.transfer.ftp.connection.Connection;
import io.linuxserver.davos.web.Host;
import io.linuxserver.davos.web.controller.response.DirectoryListing;
import io.linuxserver.davos.web.controller.response.DirectoryListing.DirectoryEntry;

@Component
public class HostServiceImpl implements HostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostServiceImpl.class);

    @Resource
    private HostDAO hostDAO;

    @Resource
    private ScheduleDAO scheduleDAO;

    @Resource
    private HostConverter hostConverter;

    @Override
    public Host fetchHost(Long id) {
        return toHost(hostDAO.fetchHost(id));
    }

    @Override
    public Host saveHost(Host host) {

        HostModel model = hostConverter.convertFrom(host);
        return hostConverter.convertTo(hostDAO.saveHost(model));
    }

    @Override
    public void deleteHost(Long id) {
        
        List<Long> schedulesUsingHost = fetchSchedulesUsingHost(id);
        
        if (schedulesUsingHost.isEmpty()) {
            hostDAO.deleteHost(id);
        } else {
            throw new HostInUseException("Host is being used by Schedules: " + schedulesUsingHost);
        }
    }

    @Override
    public List<Host> fetchAllHosts() {
        return hostDAO.fetchAllHosts().stream().map(this::toHost).collect(Collectors.toList());
    }

    private Host toHost(HostModel model) {
        return hostConverter.convertTo(model);
    }

    @Override
    public List<Long> fetchSchedulesUsingHost(Long id) {
        return scheduleDAO.fetchSchedulesUsingHost(id).stream().map(s -> s.id).collect(Collectors.toList());
    }

    @Override
    public void testConnection(Host host) {

        HostModel model = hostConverter.convertFrom(host);

        LOGGER.info("Attempting to test connection to host", model.address);

        Client client = buildClient(model);

        LOGGER.debug("Making connection on port {}", model.port);
        client.connect();
        LOGGER.info("Connection successful.");
        client.disconnect();
        LOGGER.debug("Disconnected");
    }

    @Override
    public DirectoryListing browseDirectory(Long hostId, String path) {

        HostModel model = hostDAO.fetchHost(hostId);

        LOGGER.info("Browsing directory {} on host {}", path, model.address);

        Client client = buildClient(model);
        Connection connection = client.connect();

        try {

            String currentPath = (null == path || path.trim().isEmpty()) ? connection.currentDirectory() : path.trim();

            List<DirectoryEntry> directories = connection.listFiles(currentPath).stream()
                    .filter(FTPFile::isDirectory)
                    .filter(file -> !file.getName().equals(".") && !file.getName().equals(".."))
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .map(file -> new DirectoryEntry(file.getName(), joinPath(currentPath, file.getName())))
                    .collect(Collectors.toList());

            return new DirectoryListing(currentPath, parentOf(currentPath), directories);

        } finally {
            client.disconnect();
            LOGGER.debug("Disconnected after browsing");
        }
    }

    private Client buildClient(HostModel model) {

        Client client = new ClientFactory().getClient(model.protocol);

        LOGGER.debug("Credentials: {} : {}", model.username, model.password);

        UserCredentials userCredentials;

        if (model.isIdentityFileEnabled())
            userCredentials = new UserCredentials(model.username, new Identity(model.identityFile));
        else
            userCredentials = new UserCredentials(model.username, model.password);

        client.setCredentials(userCredentials);
        client.setHost(model.address);
        client.setPort(model.port);

        return client;
    }

    private String joinPath(String base, String name) {
        return base.endsWith("/") ? base + name : base + "/" + name;
    }

    private String parentOf(String path) {

        String normalized = path;
        if (normalized.length() > 1 && normalized.endsWith("/"))
            normalized = normalized.substring(0, normalized.length() - 1);

        int lastSlash = normalized.lastIndexOf('/');

        if (lastSlash < 0)
            return null;
        if (lastSlash == 0)
            return normalized.equals("/") ? null : "/";

        return normalized.substring(0, lastSlash);
    }
}
