package co.jp.treasuredata.armtd.server.server.commands.discovery;

import co.jp.treasuredata.armtd.server.ServerConfig;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandlerMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PropertiesBasedHandlersDiscoverer implements HandlersDiscoverer {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesBasedHandlersDiscoverer.class);

    private final Properties properties;
    private final ServerConfig config;
    private final ClassLoader classLoader;

    public PropertiesBasedHandlersDiscoverer(ServerConfig config, Properties properties) {
        this(config, properties, PropertiesBasedHandlersDiscoverer.class.getClassLoader());
    }

    public PropertiesBasedHandlersDiscoverer(ServerConfig config, Properties properties, ClassLoader classLoader) {
        this.properties = properties;
        this.config = config;
        this.classLoader = classLoader;
    }

    @Override
    public List<RouteHandlerMapping> resolveHandlers() throws DiscovererException {
        final List<RouteHandlerMapping> result = new ArrayList<>();

        Enumeration<Object> keys = properties.keys();

        while(keys.hasMoreElements()) {
            String key = String.valueOf(keys.nextElement());
            if (!key.startsWith("route.")) continue;

            String className = properties.getProperty(key);

            if (className == null || className.isEmpty()) {
                continue;
            }

            final Class<?> handlerClazz;
            try {
                handlerClazz = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new DiscovererException("Handler class " + className + " is not available in the classpath", e);
            }

            if (!RouteHandler.class.isAssignableFrom(handlerClazz)) {
                throw new DiscovererException("Handler class " + className + " must be an instance of " + RouteHandler.class);
            }

            DiscoverableHandler annotation = handlerClazz.getAnnotation(DiscoverableHandler.class);
            if (annotation == null) {
                throw new DiscovererException("Handler class " + className + " must be annotated with " + DiscoverableHandler.class);
            }

            final RouteHandler handler;
            try {
                Class<? extends RouteHandler> targetClazz = handlerClazz.asSubclass(RouteHandler.class);

                Constructor<?>[] constructors = targetClazz.getConstructors();
                if (constructors.length == 0) {
                    handler = targetClazz.newInstance();
                } else {
                    Constructor<?> targetConstructor = null;
                    for (Constructor<?> constructor : constructors) {
                        if (constructor.getParameterTypes().length != 0) continue;

                        targetConstructor = constructor;
                        break;
                    }

                    boolean isConfigRequired = false;
                    for (Constructor<?> constructor : constructors) {
                        if (constructor.getParameterTypes().length == 0 ||
                                !constructor.getParameterTypes()[0].isAssignableFrom(ServerConfig.class))
                            continue;

                        targetConstructor = constructor;
                        isConfigRequired = true;
                    }

                    if (targetConstructor == null) {
                        throw new DiscovererException("At least a single empty public constructor expected on type "
                                + className);
                    } else if (isConfigRequired) {
                        handler = (RouteHandler) targetConstructor.newInstance(config);
                    } else {
                        handler = (RouteHandler) targetConstructor.newInstance();
                    }


                }
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new DiscovererException("Failure during the instantiation of the handler class " + className, e);
            }

            if (handler == null) continue;

            logger.info("New commands handler registered - " + className + " (mappings: "
                    + String.join(" ", annotation.mappings()) + ")");

            result.add(new RouteHandlerMapping(Arrays.asList(annotation.mappings()), handler));
        }

        return result;
    }
}
