package co.jp.treasuredata.armtd.server.commands.actions;

import static org.junit.Assert.assertEquals;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.server.commands.actions.ErrorResponseAction;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

public class ErrorResponseActionTest {

  @Test
  public void testErrorResponseActionEncodeDataCorrectly()
      throws ExecutionException, InterruptedException {
    Request testRequest = new Request(10923, "test-type", new String[] {});
    String errorActionDetails = "test.message.details";
    int errorCode = 10001;
    ErrorResponseAction action = new ErrorResponseAction(errorCode, errorActionDetails);
    CompletableFuture<List<Packet>> packetsFuture = action.execute(testRequest);
    List<Packet> packets = packetsFuture.get();
    assertEquals(1, packets.size());
    assertEquals(testRequest.getToken(), packets.get(0).getToken());

    String packetData = "error " + errorCode + ":" + errorActionDetails;
    assertEquals(packetData, new String(packets.get(0).getData()));
    assertEquals(packetData.getBytes().length, packets.get(0).getLength());
  }
}
