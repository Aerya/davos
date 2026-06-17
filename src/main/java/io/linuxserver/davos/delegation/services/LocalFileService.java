package io.linuxserver.davos.delegation.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.linuxserver.davos.web.controller.response.DirectoryListing;
import io.linuxserver.davos.web.controller.response.DirectoryListing.DirectoryEntry;

/**
 * Browses the local file system so the UI can pick a download directory. All
 * navigation is constrained to a configurable root (the volume mounted into the
 * container, {@code /download} by default) to avoid exposing the rest of the
 * file system.
 */
@Component
public class LocalFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileService.class);

    @Value("${davos.local.downloadRoot:/download}")
    private String downloadRoot;

    public DirectoryListing browse(String path) {

        try {

            File root = new File(downloadRoot).getCanonicalFile();

            File target = (null == path || path.trim().isEmpty()) ? root : new File(path.trim()).getCanonicalFile();

            if (!isWithinRoot(target, root)) {
                LOGGER.warn("Requested path {} is outside of the allowed root {}. Falling back to root", path, downloadRoot);
                target = root;
            }

            List<DirectoryEntry> directories = new ArrayList<>();

            File[] children = target.listFiles(File::isDirectory);
            if (null != children) {
                directories = Arrays.stream(children)
                        .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(file -> new DirectoryEntry(file.getName(), file.getPath()))
                        .collect(Collectors.toList());
            } else {
                LOGGER.warn("Unable to list directory {} (does it exist and is it readable?)", target.getPath());
            }

            String parent = target.equals(root) ? null : target.getParentFile().getPath();

            return new DirectoryListing(target.getPath(), parent, directories);

        } catch (IOException e) {
            throw new RuntimeException("Unable to browse local directory " + path, e);
        }
    }

    private boolean isWithinRoot(File target, File root) {

        File current = target;
        while (null != current) {
            if (current.equals(root))
                return true;
            current = current.getParentFile();
        }
        return false;
    }
}
