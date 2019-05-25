package co.jp.treasuredata.armtd.server.server.commands.discovery;

import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandlerMapping;

import java.util.List;

public interface HandlersDiscoverer {

    /**
     * Probes the environments based on the given discovery strategy to find all available {@link RouteHandler}
     * implementations.
     *
     * @return List of all discovered handlers with their respective mapping information
     * @throws DiscovererException If instantiation of one of {@link RouteHandler}s has failed or it is missing
     *                             {@link DiscoverableHandler} annotation as a part of its type definition.
     */
    List<RouteHandlerMapping> resolveHandlers() throws DiscovererException;

}
