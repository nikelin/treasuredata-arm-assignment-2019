package co.jp.treasuredata.armtd.server.server.commands;

import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.actions.ErrorResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscovererException;
import co.jp.treasuredata.armtd.server.server.commands.discovery.HandlersDiscoverer;

import java.util.Collections;
import java.util.List;

public final class DiscoveryRoutingRouteHandler implements RouteHandler {

    public static DiscoveryRoutingRouteHandler create(HandlersDiscoverer handlersDiscoverer) throws HandlerException {
        final List<RouteHandlerMapping> routesMapping;
        try {
            routesMapping = handlersDiscoverer.resolveHandlers();
        } catch (DiscovererException e) {
            throw new HandlerException("Unable to resolve supported handlers list", e);
        }

        return new DiscoveryRoutingRouteHandler(routesMapping);
    }

    private final List<RouteHandlerMapping> routesMapping;

    public DiscoveryRoutingRouteHandler(List<RouteHandlerMapping> routeHandlerMappings) {
        this.routesMapping = routeHandlerMappings;
    }

    @Override
    public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException {
        RouteHandler targetHandler = null;
        for (RouteHandlerMapping mapping : routesMapping) {
            if (!mapping.getMappings().contains(request.getTypeName())) continue;

            targetHandler = mapping.getHandler();
            break;
        }

        if (targetHandler == null) {
            return Collections.singletonList(new ErrorResponseAction(ErrorResponseAction.UNSUPPORTED_METHOD_CALL,
                    "unsupported method"));
        }

        return targetHandler.handleRequest(server, request);
    }
}
