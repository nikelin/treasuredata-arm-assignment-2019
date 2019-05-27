package co.jp.treasuredata.armtd.server.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListFilesResponseAction implements ResponseAction {
  private final File directory;

  public ListFilesResponseAction(File directory) {
    this.directory = directory;
  }

  public File getDirectory() {
    return directory;
  }

  @Override
  public CompletableFuture<List<Packet>> execute(Request request) {
    CompletableFuture<List<Packet>> future = new CompletableFuture<>();

    File[] directoryFiles = directory.listFiles(File::isFile);
    if (directoryFiles == null) {
      future.completeExceptionally(new IOException("failed to list directory"));
    } else {
      StringBuilder responseBuilder = new StringBuilder();
      for (File file : directoryFiles) {
        responseBuilder.append(file.getName()).append("\n\r");
      }

      future.complete(
          Collections.singletonList(
              new Packet(request.getToken(), responseBuilder.toString().getBytes())));
    }

    return future;
  }
}
