package co.jp.treasuredata.armtd.server.commands;

import static org.junit.Assert.*;

import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.DiscoveryRoutingRouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.HandlerException;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandlerMapping;
import co.jp.treasuredata.armtd.server.server.commands.actions.ErrorResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.actions.TextResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscovererException;
import co.jp.treasuredata.armtd.server.server.commands.discovery.HandlersDiscoverer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class DiscoveryRoutingRouteHandlerTest {

  @Test
  public void mustReturnErrorResponseForUnsupportedMethodCall() throws HandlerException {
    DiscoveryRoutingRouteHandler handler =
        new DiscoveryRoutingRouteHandler(Collections.emptyList());
    List<ResponseAction> actions =
        handler.handleRequest(null, new Request(0, "unknown", new String[] {}));
    assertFalse(actions.isEmpty());
    assertEquals(1, actions.size());
    assertEquals(actions.get(0).getClass(), ErrorResponseAction.class);
    assertEquals(
        ErrorResponseAction.UNSUPPORTED_METHOD_CALL,
        ((ErrorResponseAction) actions.get(0)).getCode().intValue());
  }

  @Test
  public void mustReturnErrorResponseForUnsupportedMethodCall2() throws HandlerException {
    DiscoveryRoutingRouteHandler handler =
        new DiscoveryRoutingRouteHandler(Collections.singletonList(dummyRouteHandlerMappingA));

    List<ResponseAction> actions =
        handler.handleRequest(null, new Request(0, "unknown", new String[] {}));

    assertFalse(actions.isEmpty());
    assertEquals(1, actions.size());
    assertEquals(actions.get(0).getClass(), ErrorResponseAction.class);
    assertEquals(
        ErrorResponseAction.UNSUPPORTED_METHOD_CALL,
        ((ErrorResponseAction) actions.get(0)).getCode().intValue());
  }

  @Test
  public void mustCorrectlyRouteRequestToTheMatchingHandlerMapping1() throws HandlerException {
    DiscoveryRoutingRouteHandler handler =
        new DiscoveryRoutingRouteHandler(Collections.singletonList(dummyRouteHandlerMappingA));

    List<ResponseAction> actions =
        handler.handleRequest(
            null, new Request(0, dummyRouteHandlerMappingA.getMappings().get(0), new String[] {}));

    assertFalse(actions.isEmpty());
    assertEquals(1, actions.size());
    assertEquals(actions.get(0).getClass(), TextResponseAction.class);
    assertEquals(((TextResponseAction) actions.get(0)).getMessage(), "dummyA");
  }

  @Test
  public void mustCorrectlyRouteRequestToTheMatchingHandlerWithMultipleMappings()
      throws HandlerException {
    DiscoveryRoutingRouteHandler handler =
        new DiscoveryRoutingRouteHandler(
            Arrays.asList(
                dummyRouteHandlerMappingA, dummyRouteHandlerMappingB, dummyRouteHandlerMappingC));

    List<ResponseAction> actions =
        handler.handleRequest(
            null, new Request(0, dummyRouteHandlerMappingB.getMappings().get(0), new String[] {}));

    assertFalse(actions.isEmpty());
    assertEquals(1, actions.size());
    assertEquals(actions.get(0).getClass(), TextResponseAction.class);
    assertEquals(((TextResponseAction) actions.get(0)).getMessage(), "dummyB");
  }

  @Test
  public void factoryMethodsCorrectlyUtilisesProvidedDiscovererAndConstructsRoutesHandler()
      throws HandlerException {
    DiscoveryRoutingRouteHandler handler =
        DiscoveryRoutingRouteHandler.create(dummyHandlersDiscoverer);

    List<ResponseAction> actions =
        handler.handleRequest(
            null, new Request(0, dummyRouteHandlerMappingA.getMappings().get(0), new String[] {}));

    assertFalse(actions.isEmpty());
    assertEquals(1, actions.size());
    assertEquals(actions.get(0).getClass(), TextResponseAction.class);
    assertEquals(((TextResponseAction) actions.get(0)).getMessage(), "dummyA");
  }

  private static final HandlersDiscoverer dummyHandlersDiscoverer =
      new HandlersDiscoverer() {
        @Override
        public List<RouteHandlerMapping> resolveHandlers() throws DiscovererException {
          return Collections.singletonList(dummyRouteHandlerMappingA);
        }
      };

  private static final RouteHandlerMapping dummyRouteHandlerMappingA =
      new RouteHandlerMapping(
          Collections.singletonList("dummyA"),
          (s, request) -> Collections.singletonList(new TextResponseAction("dummyA")));

  private static final RouteHandlerMapping dummyRouteHandlerMappingB =
      new RouteHandlerMapping(
          Arrays.asList("dummyB", "dummyB-B"),
          (s, request) -> Collections.singletonList(new TextResponseAction("dummyB")));

  private static final RouteHandlerMapping dummyRouteHandlerMappingC =
      new RouteHandlerMapping(
          Collections.singletonList("dummyC"),
          (s, request) -> Collections.singletonList(new TextResponseAction("dummyC")));
}
