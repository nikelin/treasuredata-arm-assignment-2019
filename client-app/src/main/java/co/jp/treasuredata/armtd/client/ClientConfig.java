package co.jp.treasuredata.armtd.client;

import org.kohsuke.args4j.Option;

public class ClientConfig {

    @Option(name = "-h", aliases = "--host", usage = "A hostname of a TD file server")
    private String serverHost;

    @Option(name = "-p", aliases = "--port", required = true, usage = "A port of a TD file server")
    private Integer serverPort;

    public String getServerHost() {
        return serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }
}
