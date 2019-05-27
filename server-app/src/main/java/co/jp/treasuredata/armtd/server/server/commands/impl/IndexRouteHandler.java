package co.jp.treasuredata.armtd.server.server.commands.impl;

import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.ServerConfig;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.actions.ListFilesResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscoverableHandler;
import java.io.File;
import java.util.Collections;
import java.util.List;

@DiscoverableHandler(
    mappings = {"index"},
    description = "Command to list all files from the root directory specified on startup")
public class IndexRouteHandler implements RouteHandler {

  private final ServerConfig config;

  public IndexRouteHandler(ServerConfig config) {
    this.config = config;
  }

  @Override
  public List<ResponseAction> handleRequest(Server server, Request request) {
    return Collections.singletonList(
        new ListFilesResponseAction(new File(config.getInputDirectoryPath())));
  }
}
