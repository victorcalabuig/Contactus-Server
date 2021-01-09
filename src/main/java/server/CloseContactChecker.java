package server;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Thread secundario que cada cierto tiempo comprueba todos los contactos cercanos de
 * usuarios suspect e infected.
 */
public class CloseContactChecker extends ServerInstance implements Runnable {

    Connection con;

    /**
     * Tiempo entre cada checkeo de contactos cercanos. Debe ser relativamente alto ya que
     * la comprobación de contactos cercanos implica mucha carga de trabajo.
     */
    int sleepTime = 30000;

    public CloseContactChecker(Connection con){
        this.con = con;
    }

    /**
     * Cada cierto tiempo (sleepTime) ejecuta el método checkCloseContacts.
     */
    public void run(){

        try {
            Statement stmt = con.createStatement();

            while(true){
                Thread.sleep(sleepTime);

                System.out.println("[INFO] Checking close contacts of all suspect and infected users...");
                checkCloseContacts(stmt);
            }
        } catch (SQLException | InterruptedException e){
            e.printStackTrace();
        }
    }
}
