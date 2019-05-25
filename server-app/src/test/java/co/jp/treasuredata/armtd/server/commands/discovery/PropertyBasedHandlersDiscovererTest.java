package co.jp.treasuredata.armtd.server.commands.discovery;

import static org.junit.Assert.*;

import co.jp.treasuredata.armtd.server.ServerConfig;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.HandlerException;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandlerMapping;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscoverableHandler;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscovererException;
import co.jp.treasuredata.armtd.server.server.commands.discovery.PropertiesBasedHandlersDiscoverer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class PropertyBasedHandlersDiscovererTest {

    @Test
    public void mustIgnoreAllIrrelevantLines() throws DiscovererException {
        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(null,
                onlyIrrelevantLinesConfig);

        assertTrue(discoverer.resolveHandlers().isEmpty());
    }

    @Test(expected = DiscovererException.class)
    public void mustThrowExceptionWhenClassNameIsUnavailableOnClasspath() throws DiscovererException {
        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(null,
                missingClassNameConfig);

        discoverer.resolveHandlers();
    }

    @Test(expected = DiscovererException.class)
    public void mustThrowExceptionWhenClassNameIsNotAnInstanceOfRouteHandler() throws DiscovererException {
        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(null,
                isNotAnInstanceOfRouteHandler, PropertyBasedHandlersDiscovererTest.class.getClassLoader());

        discoverer.resolveHandlers();
    }

    @Test(expected = DiscovererException.class)
    public void mustThrowExceptionWhenClassNameIsMissingRequiredAnnotation() throws DiscovererException {
        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(null,
                missingRequiredAnnotation, PropertyBasedHandlersDiscovererTest.class.getClassLoader());

        discoverer.resolveHandlers();
    }

    @Test(expected = DiscovererException.class)
    public void mustThrowExceptionWhenClassNameIsMissingCompatibleConstructor() throws DiscovererException {
        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(null,
                missingCompatibleConstructor, PropertyBasedHandlersDiscovererTest.class.getClassLoader());

        discoverer.resolveHandlers();
    }

    @Test
    public void mustBeAbleToIdentifyCorrectRouteHandlerInstances() throws DiscovererException {
        ServerConfig dummyConfig = new ServerConfig();

        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(dummyConfig,
                correctDummyHandler, PropertyBasedHandlersDiscovererTest.class.getClassLoader());

        List<RouteHandlerMapping> handlers = discoverer.resolveHandlers();
        assertEquals(1, handlers.size());
        assertEquals(CorrectDummyHandler.class, handlers.get(0).getHandler().getClass());
        assertEquals(dummyConfig, ((CorrectDummyHandler) handlers.get(0).getHandler()).config);
        assertEquals(1, handlers.get(0).getMappings().size());
    }

    @Test
    public void mustBeAbleToIdentifyCorrectRouteHandlerInstancesNotRequiringConfig() throws DiscovererException {
        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(null,
                correctDummyHandlerWithoutConfig, PropertyBasedHandlersDiscovererTest.class.getClassLoader());

        List<RouteHandlerMapping> handlers = discoverer.resolveHandlers();
        assertEquals(1, handlers.size());
        assertEquals(CorrectDummyHandlerWithoutConfig.class, handlers.get(0).getHandler().getClass());
        assertEquals(3, handlers.get(0).getMappings().size());
    }

    @Test
    public void mustBeAbleToIdentifyCorrectRouteHandlerInstancesWithDefaultConstructor() throws DiscovererException {
        PropertiesBasedHandlersDiscoverer discoverer = new PropertiesBasedHandlersDiscoverer(null,
                correctDummyHandlerWithDefaultConstructor, PropertyBasedHandlersDiscovererTest.class.getClassLoader());

        List<RouteHandlerMapping> handlers = discoverer.resolveHandlers();
        assertEquals(1, handlers.size());
        assertEquals(CorrectDummyHandlerWithDefaultConstructor.class, handlers.get(0).getHandler().getClass());
        assertEquals(1, handlers.get(0).getMappings().size());
    }

    private static Properties createProperties(String lines) {
        try {
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(lines.getBytes()));
            return properties;
        } catch (IOException e) {
            return null;
        }
    }

    @DiscoverableHandler(mappings = {"C"})
    public static class CorrectDummyHandlerWithDefaultConstructor implements RouteHandler {
        @Override
        public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException {
            return null;
        }
    }

    @DiscoverableHandler(mappings = {"C"})
    public static class CorrectDummyHandler implements RouteHandler {
        public ServerConfig config;

        public CorrectDummyHandler() {}
        public CorrectDummyHandler(ServerConfig config) {
            this.config = config;
        }

        @Override
        public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException {
            return null;
        }
    }

    @DiscoverableHandler(mappings = {"A", "B", "C"})
    public static class CorrectDummyHandlerWithoutConfig implements RouteHandler {
        public CorrectDummyHandlerWithoutConfig() {}

        @Override
        public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException {
            return null;
        }
    }

    public static class DummyHandlerWithNoAnnotation implements RouteHandler {
        public DummyHandlerWithNoAnnotation(String argument) {}

        @Override
        public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException { return null; }
    }

    @DiscoverableHandler(mappings = {})
    public static class DummyHandlerWithNoRouteHandler implements RouteHandler {
        public DummyHandlerWithNoRouteHandler(String argument) {}

        @Override
        public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException { return null; }
    }

    @DiscoverableHandler(mappings = {})
    public static class DummyHandlerWithNoCompatibleConstructor implements RouteHandler {
        public DummyHandlerWithNoCompatibleConstructor(String argument) {}

        @Override
        public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException { return null; }
    }


    private final static Properties correctDummyHandlerWithoutConfig = createProperties(
            "route.className = co.jp.treasuredata.armtd.server.commands.discovery.PropertyBasedHandlersDiscovererTest$CorrectDummyHandlerWithoutConfig");
    private final static Properties correctDummyHandlerWithDefaultConstructor = createProperties(
            "route.className = co.jp.treasuredata.armtd.server.commands.discovery.PropertyBasedHandlersDiscovererTest$CorrectDummyHandlerWithDefaultConstructor");
    private final static Properties correctDummyHandler = createProperties(
            "route.className = co.jp.treasuredata.armtd.server.commands.discovery.PropertyBasedHandlersDiscovererTest$CorrectDummyHandler");
    private final static Properties missingRequiredAnnotation = createProperties(
            "route.className = co.jp.treasuredata.armtd.server.commands.discovery.PropertyBasedHandlersDiscovererTest$DummyHandlerWithNoAnnotation");
    private final static Properties isNotAnInstanceOfRouteHandler = createProperties(
            "route.className = co.jp.treasuredata.armtd.server.commands.discovery.PropertyBasedHandlersDiscovererTest$DummyHandlerWithNoRouteHandler");
    private final static Properties missingCompatibleConstructor = createProperties(
            "route.className = co.jp.treasuredata.armtd.server.commands.discovery.PropertyBasedHandlersDiscovererTest$DummyHandlerWithNoCompatibleConstructor");
    private final static Properties missingClassNameConfig = createProperties("route.className = x.y.Z");
    private final static Properties onlyIrrelevantLinesConfig = createProperties("x.x = ss\n\r" + "y.x = ss\n\r");

}
