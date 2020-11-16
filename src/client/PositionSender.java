package client;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.net.Socket;
import java.io.IOException;
import java.util.Random;

import utils.ImprovedNoise;
/**
* Esta clase implementa la interfaz runnable y se ejecuta en un Thread 
* secundario cuando desde el cliente se ejecuta el comando startPositions.
*/
public class PositionSender implements Runnable {

private static int userId;

//valores prueba, en la realidad debería obtenerse de alguna forma.

// Podríamos usar Perlin noise para que los valores de la posición estén cercanos entre si
private static ImprovedNoise n1;
private static double latitude = ImprovedNoise.randNoise();
private static double longitude = ImprovedNoise.randNoise();

public PositionSender(int userId){
	this.userId = userId;
}

public void run(){
	try{
		Socket clientSocket = new Socket("localhost", 8000);

		PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
		BufferedReader in = new BufferedReader(
			new InputStreamReader(clientSocket.getInputStream()));

		boolean execute = true;
		while(execute){
			try{
				//calculatePosition //faltaría obtener la geolocalizacion.
				sendPosition(out);
				Thread.sleep(5000);
			} catch(InterruptedException e){
				//System.out.println("The thread of the user " + userId + "has been interrupted");
				return;
			}
		}
	} catch (IOException e){
	}
}

private static void sendPosition(PrintWriter out){
	String msgToServer = userId + " addPosition " + latitude + " " + longitude;
	out.println(msgToServer);
	out.flush();
	//System.out.println("Message sent by a thread!!!!");
}

}