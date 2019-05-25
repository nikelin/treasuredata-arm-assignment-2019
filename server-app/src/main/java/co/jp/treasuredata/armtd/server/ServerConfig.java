package co.jp.treasuredata.armtd.server;

import org.kohsuke.args4j.Option;

public final class ServerConfig {
    @Option(name = "-d", aliases = "--directory", usage = "Input directory path", required = true)
    private String inputDirectoryPath;

    @Option(name = "-p", aliases = "--port", usage = "NIO2Server port", required = false)
    private Integer port;

    public String getInputDirectoryPath() {
        return inputDirectoryPath;
    }

    public Integer getPort() {
        return port;
    }
}