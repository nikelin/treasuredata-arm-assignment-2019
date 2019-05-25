package co.jp.treasuredata.armtd.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.server.commands.actions.FileResponseAction;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FileResponseActionTest {

    @Test
    public void testFileResponseActionFetchingCorrectFile() throws IOException, InterruptedException, ExecutionException {
        File temporaryFile = File.createTempFile("test", "test");
        temporaryFile.createNewFile();

        BufferedWriter temporaryFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temporaryFile)));
        String temporaryFileData = "test data - " + Math.random();
        temporaryFileWriter.write(temporaryFileData);
        temporaryFileWriter.close();

        Request request = new Request(10002, "test", new String[] {});
        FileResponseAction action = new FileResponseAction(temporaryFile);
        CompletableFuture<List<Packet>> future = action.execute(request);

        List<Packet> packets = future.get();
        assertEquals(1, packets.size());
        assertEquals(request.getToken(), packets.get(0).getToken());
        Packet packet = packets.get(0);
        assertArrayEquals(temporaryFileData.getBytes(), packet.getData());
    }

    @Test(expected = IOException.class)
    public void testFileResponseActionHandlingNonExistingFile() throws Throwable {
        Request request = new Request(10002, "test", new String[] {});
        FileResponseAction action = new FileResponseAction(new File("unknown-file"));
        CompletableFuture<List<Packet>> future = action.execute(request);

        try {
            future.get();
        } catch (ExecutionException e) {
            throw e.getCause().getCause();
        }
    }
}
