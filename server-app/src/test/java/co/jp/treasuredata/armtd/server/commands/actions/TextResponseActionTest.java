package co.jp.treasuredata.armtd.server.commands.actions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.server.commands.actions.TextResponseAction;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

public class TextResponseActionTest {

  @Test
  public void testTextResponseActionOutput() throws InterruptedException, ExecutionException {
    String testMessageText = "test message response";
    TextResponseAction action = new TextResponseAction(testMessageText);

    Request request = new Request(0, "testType", new String[] {});

    CompletableFuture<List<Packet>> future = action.execute(request);
    List<Packet> packets = future.get();
    assertEquals(1, packets.size());
    assertArrayEquals(testMessageText.getBytes(), packets.get(0).getData());
    assertEquals(request.getToken(), packets.get(0).getToken());
  }
}
