package io.linuxserver.davos.web.controller.response;

import java.util.List;

/**
 * Represents the contents of a single directory when browsing either a remote
 * host or the local file system. Only sub-directories are listed, since davos
 * schedules only ever target directories.
 */
public class DirectoryListing {

    public String path;
    public String parent;
    public List<DirectoryEntry> directories;

    public DirectoryListing(String path, String parent, List<DirectoryEntry> directories) {
        this.path = path;
        this.parent = parent;
        this.directories = directories;
    }

    public static class DirectoryEntry {

        public String name;
        public String path;

        public DirectoryEntry(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}
