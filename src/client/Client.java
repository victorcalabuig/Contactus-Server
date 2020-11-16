package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

/**
* Guarda el Id del usuario actual, y es 0 hasta que se inicie sesión con el 
* comando Login. Sirve para indicarle al servidor qué usuario hay detrás del 
* cliente.
*/
static private int currentUserId = 0;

/**
* Guarda el username actual para utilizarlo en el prompt.
*/
static private String currentUsername = "";

/**
* Thread utilizado para enviar posicioes automaticamente cuando se utiliza 
* el comando startPositions.
*/
static private Thread posSenderThread;

static private boolean debug = false;

/**
* Comprueba si el comando login ha sido exitoso. En caso de exito, actualiza
* el currentUserId y el currentUsername, abriendo así una sesión de usuario.
* @param fields Contestación del servidor separada por espacios a la llamada
* de login.
*/
private static void processLoginResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0 && fields.length == 4){
		currentUserId = Integer.valueOf(fields[2]);
		currentUsername = fields[3];
	}
}

private static void processLogoutResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0){
		currentUserId = 0;
		currentUsername = "";
		stopPositions();
	}
}


/**
* Comprubea si comando listUsers ha sido exitoso. En caso afirmativo, imprime
* por pantalla los usuarios contenidos en el mensaje del servidor.
* @param fields Mensaje del servidor.
*/
private static void processListUsersResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0){
		for(int i = 2; i < fields.length; i++)
			System.out.print(fields[i] + " ");
		System.out.println();
	}
}


/**
* Comprueba si listPositions ha tenido exito. En caso afirmatiov, imprime las 
* posiciones recibidas del servidor, que siempre estan en la tercera posicion
* del mensaje fields.
* @param fields Mensaje del servidor. 
*/
private static void processListPositionsResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0 && fields.length > 2){
		//Print header of table
		System.out.printf("%-25s%-5s%-15s%-5s%-15s\n", "Time", "|", "Latitude", "|", "Longitude");
		System.out.println("------------------------------------------------------------");

		String[] positions = fields[2].split("//");		
		for(int i = 0; i < positions.length; i++){
			printPosition(positions[i]);
			System.out.println();
		}
	}
}

/**
* Imprime una posición, separandola la posicion en fecha, latitud y longitud.
*/
private static void printPosition(String position){
	String[] fields = position.split("\\|");
	System.out.printf("%-25s", fields[0].replace("_"," "));
	System.out.printf("%-5s", "|");
	System.out.printf("%-15s", fields[1]);
	System.out.printf("%-5s", "|");
	System.out.printf("%-15s", fields[2]);
}


/**
* Crea una nueva instancia de la clase PositionSender e inicia un thread con 
* el metodo run de la clase PositionSender.
*/
private static void startPositions(){
	PositionSender ps = new PositionSender(currentUserId);
	posSenderThread = new Thread(ps);
	posSenderThread.setDaemon(true);
	posSenderThread.start();
}

/**
* Si la llamada startPositions ha sido exitosa, llama al método startPositions.
*/
private static void processStartPositionsResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0) startPositions();
}


/**
* Manda un interrupt al thread posSenderThread si este este activo.
*/
private static void stopPositions(){
	if(posSenderThread != null && posSenderThread.isAlive()){
		posSenderThread.interrupt();
	}
}

/**
* LLama a stopPositions si el resultado del comando stopPositions es exitoso (0).
*/
private static void processStopPositionsResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0) stopPositions();
}


public static void main(String[] args) throws IOException {
	Scanner keyboard = new Scanner(System.in);
	Socket clientSocket = new Socket("localhost", 8000);

	PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
	BufferedReader in = new BufferedReader(
		new InputStreamReader(clientSocket.getInputStream()));

	boolean execute = true;
	while(execute){
		System.out.print(currentUsername + "> ");
		String keyboardInput = keyboard.nextLine(); 
		//Se añade como cabecera el Id del usuario actual
		String msgToServer = currentUserId + " " + keyboardInput; 

		//Envio de mensaje al servidor
	    out.println(msgToServer); //
	    out.flush();

	    //Mecanismo temporal para cerrar la conexión
	    if(keyboardInput.equals("close")) break; 

	    //Resultado que nos envía el servidor
	    String res = in.readLine();
	    if(debug)
	    	System.out.println("server response: " + res); //Comprobación

	    //Implementar lógica tras contestación
	    //Formato de mensaje recibido: "comando_ejecutado resultado info1 info2 ...."
	    String[] fields = res.split(" ");
	    if(fields.length > 0){
	    	switch(fields[0]){
	    		case "login": 
	    			processLoginResult(fields);
	    			break;
	    		case "logout":
	    			processLogoutResult(fields);
	    			break;
	    		case "listUsers": 
	    			processListUsersResult(fields);
	    			break;
	    		case "listPositions":
	    			processListPositionsResult(fields);
	    			break;
	    		case "startPositions":
	    			processStartPositionsResult(fields);
	    			break;
	    		case "stopPositions":
	    			processStopPositionsResult(fields);
	    			break;
	    		case "exit":
	    			execute = false;
	    			break;
	    		case "debug":
	    			debug = !debug;
	    			break;
	    	}
	    }
	}
	    
	clientSocket.close();
}

}