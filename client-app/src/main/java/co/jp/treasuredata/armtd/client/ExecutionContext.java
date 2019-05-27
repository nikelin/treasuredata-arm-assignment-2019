package co.jp.treasuredata.armtd.client;

import co.jp.treasuredata.armtd.client.api.TDServerConnector;
import java.io.BufferedReader;
import java.io.PrintStream;

public class ExecutionContext {
  private final ClientConfig clientConfig;
  private final TDServerConnector connector;
  private final BufferedReader in;
  private final PrintStream out;

  public ExecutionContext(
      ClientConfig clientConfig, TDServerConnector connector, BufferedReader in, PrintStream out) {
    this.clientConfig = clientConfig;
    this.connector = connector;
    this.in = in;
    this.out = out;
  }

  public BufferedReader in() {
    return in;
  }

  public PrintStream out() {
    return out;
  }

  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  public TDServerConnector getConnector() {
    return connector;
  }
}
