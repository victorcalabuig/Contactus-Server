package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {

	public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(8000);
        System.out.println("Waiting for client conexions...");
        Socket clientSocket = serverSocket.accept();
        
        BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(
            clientSocket.getOutputStream(), true);
        
        while(true){
            String rec = in.readLine();
            if(rec.equals("close")) break;
            //out.println(rec);
            //Implementar lógica aquí
        }
        
        clientSocket.close();
        serverSocket.close();
    }

}