package client;

import org.junit.Assert;
import org.junit.Test;
import server.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;

/* Este test lanza un servidor, se conecta a él y ejecuta los comandos en src/test/resources/clientTest, una vez
ejecutados, compara el resultado de la ejecución tanto desde el cliente como desde el servidor con la salida esperada
 */
public class ClientTest {
    @Test
    public void main() throws IOException {
        serverThread s1 = new serverThread();
        System.setIn(new FileInputStream("src/test/resources/clientTest"));
        System.setOut(new PrintStream("src/test/resources/clientTestResults"));
        s1.start();
        try {
            Client.main(null);
        }catch (Exception ignored){
            byte[] b1 = Files.readAllBytes(Path.of("src/test/resources/clientTestResults"));
            byte[] b2 = Files.readAllBytes(Path.of("src/test/resources/clientTestResultsExpected"));
            if (!Arrays.equals(b1,b2)){
                Assert.fail();
            }
            s1.interrupt();
        }
    }
}

class serverThread extends Thread{
    public void run(){
        try {
            Server.main(null);
        } catch (IOException | InterruptedException | SQLException e) {
            e.printStackTrace();
        }
    }
}
