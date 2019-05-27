package co.jp.treasuredata.armtd.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class SocketUtils {
  public static int nextFreePort() {
    return nextFreePort(49152, 65535);
  }

  public static int nextFreePort(int from, int to) {
    Random random = new Random();
    int port = random.ints(1, from, to).findFirst().getAsInt();
    while (true) {
      if (isLocalPortFree(port)) {
        return port;
      } else {
        port = ThreadLocalRandom.current().nextInt(from, to);
      }
    }
  }

  private static boolean isLocalPortFree(int port) {
    try {
      new ServerSocket(port).close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
