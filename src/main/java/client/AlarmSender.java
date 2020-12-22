package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class AlarmSender extends Client implements Runnable {

    private static int userId;

    private static int timeInterval = 5000;

    public AlarmSender(int userId){
        this.userId = userId;
    }

    public void run(){
        try{
            Socket clientSocket = new Socket("localhost", 8000);

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            while(true){
                try{
                    sendCommand("listAlarms", out);
                    processServerResponse(in);
                    Thread.sleep(timeInterval);
                } catch(InterruptedException|IOException e){
                    //System.out.println("The thread of the user " + userId + "has been interrupted");
                    return;
                }
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    private static void sendCommand(String command, PrintWriter out){
        out.println(String.format("%d %s", userId, command));
        out.flush();
    }

    private static void processServerResponse(BufferedReader in) throws IOException {
        String[] fields = in.readLine().trim().split(" ");
        if(fields.length > 0){
            switch (fields[0]){
                case "listAlarms":
                    processListAlarmsResult(fields);
                    break;
            }
        }
    }

    public static void processListAlarmsResult(String[] fields){
        if(commandSuccess(fields) && fields.length > 2){
            String[] alarmsArr = Arrays.copyOfRange(fields, 2, fields.length);
            System.out.println(String.join(" ", alarmsArr));
        }
    }

}
