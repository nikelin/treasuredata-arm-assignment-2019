package co.jp.treasuredata.armtd.server.commands.routes;

import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.ServerConfig;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.HandlerException;
import co.jp.treasuredata.armtd.server.server.commands.actions.ErrorResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.actions.FileResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.actions.TextResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.impl.GetRouteHandler;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GetRouteHandlerTest {

    @Test
    public void testRequestForExistingFile() throws IOException, HandlerException {
        File temporaryDirectory = new File("./test");
        temporaryDirectory.deleteOnExit();
        temporaryDirectory.mkdir();

        File temporaryDirectoryFile = new File(temporaryDirectory, "test-file.pdf");
        temporaryDirectoryFile.createNewFile();


        ServerConfig config = new ServerConfig(temporaryDirectory.getAbsolutePath(), null, null);
        GetRouteHandler handler = new GetRouteHandler(config);
        List<ResponseAction> actions = handler.handleRequest(null,
                new Request(0, "get", new String[] { temporaryDirectoryFile.getName()}));

        assertEquals(2, actions.size());
        assertEquals(TextResponseAction.class, actions.get(0).getClass());
        assertEquals(FileResponseAction.class, actions.get(1).getClass());
    }

    @Test
    public void testRequestForNonExistingFile() throws IOException, HandlerException {
        File temporaryDirectory = new File("./test-" + Math.random());
        temporaryDirectory.deleteOnExit();
        temporaryDirectory.mkdir();

        File temporaryDirectoryFile = new File(temporaryDirectory, "test-file.pdf");
        temporaryDirectoryFile.createNewFile();
        temporaryDirectoryFile.deleteOnExit();

        ServerConfig config = new ServerConfig(temporaryDirectory.getAbsolutePath(), null, null);
        GetRouteHandler handler = new GetRouteHandler(config);
        List<ResponseAction> actions = handler.handleRequest(null,
                new Request(0, "get", new String[] { "non-existing-file.pdf" }));

        assertEquals(1, actions.size());
        assertEquals(ErrorResponseAction.class, actions.get(0).getClass());
    }

}
