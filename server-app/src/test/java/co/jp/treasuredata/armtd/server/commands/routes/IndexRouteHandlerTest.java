package co.jp.treasuredata.armtd.server.commands.routes;

import static org.junit.Assert.assertEquals;

import co.jp.treasuredata.armtd.server.ServerConfig;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.actions.ListFilesResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.impl.IndexRouteHandler;
import java.util.List;
import org.junit.Test;

public class IndexRouteHandlerTest {

  @Test
  public void testIndexRoute() {
    ServerConfig config = new ServerConfig("./input-directory", null, null);
    IndexRouteHandler handler = new IndexRouteHandler(config);
    List<ResponseAction> result = handler.handleRequest(null, null);
    assertEquals(1, result.size());
    assertEquals(ListFilesResponseAction.class, result.get(0).getClass());
    assertEquals(
        "input-directory", ((ListFilesResponseAction) result.get(0)).getDirectory().getName());
  }
}
