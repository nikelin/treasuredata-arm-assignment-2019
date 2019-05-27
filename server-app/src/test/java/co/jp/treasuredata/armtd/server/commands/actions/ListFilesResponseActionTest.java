package co.jp.treasuredata.armtd.server.commands.actions;

import static org.junit.Assert.assertEquals;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.server.commands.actions.ListFilesResponseAction;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;

public class ListFilesResponseActionTest {

  @Test
  public void testFilesListingForExistingDirectory()
      throws IOException, InterruptedException, ExecutionException {
    File tempFileDirectory = new File("./temp-directory-" + Math.random());
    tempFileDirectory.mkdir();
    tempFileDirectory.deleteOnExit();

    List<String> expectedFilesList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String fileName = i + ".pdf";
      File testFile = new File(tempFileDirectory, fileName);
      testFile.createNewFile();
      testFile.deleteOnExit();
      expectedFilesList.add(fileName);
    }

    int testToken = 10023;
    Request request = new Request(testToken, "test", new String[] {});
    ListFilesResponseAction action = new ListFilesResponseAction(tempFileDirectory);
    CompletableFuture<List<Packet>> future = action.execute(request);
    List<Packet> packets = future.get();

    assertEquals(1, packets.size());
    assertEquals(testToken, packets.get(0).getToken());

    String data = new String(packets.get(0).getData());
    String[] filesList = data.split("\n\r");
    assertEquals(10, filesList.length);
    Assert.assertTrue(
        "all files are present", Arrays.asList(filesList).containsAll(expectedFilesList));
  }

  @Test(expected = IOException.class)
  public void testFilesListingForNonExistingDirectory() throws Throwable {
    try {
      int testToken = 10023;
      Request request = new Request(testToken, "test", new String[] {});
      ListFilesResponseAction action =
          new ListFilesResponseAction(new File("non-existing-directory"));
      CompletableFuture<List<Packet>> future = action.execute(request);
      future.get();
    } catch (Throwable e) {
      throw e.getCause();
    }
  }
}
