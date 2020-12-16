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
private static double latitude = 0;
private static double longitude = 0;

public PositionSender(int userId){
	PositionSender.userId = userId;
}

public void run(){
	try{
		Socket clientSocket = new Socket("localhost", 8000);

		PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
		BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		while(true){
			try{
				calculatePosition(0.1);
				sendPosition(out);
				Thread.sleep(1000);
			} catch(InterruptedException e){
				//System.out.println("The thread of the user " + userId + "has been interrupted");
				return;
			}
		}
	} catch (IOException e){
		System.out.println(e.getMessage());
	}
}

	/**
	 * Calcula posiciones aleatorias pero cercanas entre si
	 * @param distance Regula la separación MAXIMA entre los valores
	 */
	private static void calculatePosition(double distance){
	ImprovedNoise imprN = new ImprovedNoise();
	Random r1 = new Random();
	if (latitude == 0){ //Inicia la posición en un punto aleatorio del mapa
		latitude = map(imprN.noise(1),-1,1,-90,90);
		longitude = map(imprN.noise(2),-1,1,-180,180);
	}
	int nextOp = r1.nextInt(3);
		switch (nextOp){ //Decidimos si movernos solo en la latitud, longitud o en ambas
		case 0:
			latitude += map(imprN.noise(1),-1,1,-distance/2,distance/2);
			break;
		case 1:
			longitude += map(imprN.noise(1),-1,1,-distance/2,distance/2);
			break;
		case 2:
			latitude += map(imprN.noise(1),-1,1,-distance/2,distance/2);
			longitude += map(imprN.noise(1),-1,1,-distance/2,distance/2);
			break;
		case 3:
			latitude -= map(imprN.noise(1),-1,1,-distance/2,distance/2);
			longitude -= map(imprN.noise(1),-1,1,-distance/2,distance/2);
			break;
	}
	//Comprobamos que no salimos de los límites y si salimos de ellos, nos quedamos en ellos.
	if (latitude>90){
		latitude=90;
	}else if (latitude<-90){
		latitude=-90;
	}else if (longitude>180){
		longitude=180;
	}else if (longitude<-180){
		longitude=-180;
	}
}
/**
	 * Toma una variable y la define entre el rango especificado
	 * @param val		Valor de entrada
	 * @param in_min	Valor mínimo que toma la variable
	 * @param in_max	Valor máximo que toma la variable
	 * @param out_min	Valor mínimo que queremos que tome
	 * @param out_max	Valor máximo que queremos que tome
	 * @return variable de entrada acotada entre dos valores definidos
	 */
private static double map(double val, double in_min, double in_max, double out_min, double out_max) {
		return (val - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

private static void sendPosition(PrintWriter out){
	String msgToServer = userId + " addPosition " + latitude + " " + longitude;
	out.println(msgToServer);
	out.flush();
	//System.out.println("Message sent by a thread!!!!");
}

}