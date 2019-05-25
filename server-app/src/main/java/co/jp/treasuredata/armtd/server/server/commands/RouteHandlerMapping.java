package co.jp.treasuredata.armtd.server.server.commands;

import java.util.List;

public final class RouteHandlerMapping {

    private final List<String> mappings;
    private final RouteHandler handler;

    public RouteHandlerMapping(List<String> mappings, RouteHandler handler) {
        this.mappings = mappings;
        this.handler = handler;
    }

    public List<String> getMappings() {
        return mappings;
    }

    public RouteHandler getHandler() {
        return handler;
    }
}
