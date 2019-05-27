package co.jp.treasuredata.armtd.server;

import org.kohsuke.args4j.Option;

public final class ServerConfig {
  @Option(name = "-d", aliases = "--directory", usage = "Input directory path", required = true)
  private String inputDirectoryPath;

  @Option(name = "-h", aliases = "--host", usage = "Server port", required = false)
  private String host;

  @Option(name = "-p", aliases = "--port", usage = "Server host", required = false)
  private Integer port;

  public ServerConfig() {}

  public ServerConfig(String inputDirectoryPath, String host, Integer port) {
    this.inputDirectoryPath = inputDirectoryPath;
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public String getInputDirectoryPath() {
    return inputDirectoryPath;
  }

  public Integer getPort() {
    return port;
  }
}
