import database.DatabaseCreation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;

import java.net.Socket;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * Clase para definir tests relacionados con el servidor. La idea es que cada método de esta
 * clase con el sufijo TEST es un test individual que se puede llamar desde el metodo main,
 * pero esta en proceso de idea. De momento solo he puesto un test: listAlarmsTest.
 * Extiend a ResultSpec, donde se guardan todas las variables comunes (de cara a nuevos tests).
 * Para ejecutar este test hay que activar los asserts de Java (por defecto desactivados). Más
 * instrucciones en el javadoc del método main de esta clase.
 */
public class ServerSpec extends ResultsSpec {

    //Los método que se llaman igual que un comando (infected(), logout(), listAlarms(), etc. lo que hacen
    //es construir y devolver un string equivalente al comando que mandaría un cliente.

    private static int currentUserId = 0;


    public static String addPosition(String latitude, String longitude){
        return String.format("%d addPosition %s %s", currentUserId, latitude, longitude);
    }

    private static String addUser(String username, String password) {
        return String.format("%d %s %s", currentUserId, username, password);
    }

    private static String addUser(int userIndex){
        return String.format("%d addUser %s %s", currentUserId, userList[userIndex][0], userList[userIndex][1]);
    }

    private static String addUser(){
        return String.format("%d addUser %s %s", currentUserId, userList[0][0], userList[0][1]);
    }

    private static String buildErrorMessage(String command, String messageSent, String expectedAnswer, String serverAnswer){
        return String.format("Command %s failed:\n\tMessage sent: %s\n\tExpected answer: %s\n\tReceived answer: %s\n",
                command, messageSent, expectedAnswer, serverAnswer);
    }

    /**
     * Intenta borrar un fichero. Utilizado para borrar la base de datos sqlite.
     */
    public static boolean deleteFile(String filename) {
        File file = new File(filename);
        if(file.delete()) {
            System.out.println("Deleted " + filename);
            return true;
        }
        else {
            System.out.println("Could not delete " + filename);
            return false;
        }
    }

    private static String infected(){
        return String.format("%s infected", currentUserId);
    }

    public static String listAlarms(){
        return String.format("%s listAlarms", currentUserId);
    }

    public static String login(int userIndex){
        String messageSent = String.format("%s login %s %s", currentUserId, userList[userIndex][0], userList[userIndex][1]);
        currentUserId = userIndex+1;
        return messageSent;
    }

    public static String logout(){
        String messageSent = String.format("%s logout", currentUserId);
        currentUserId = 0;
        return messageSent;
    }

    public static void printTestTrace(String messageSent, String serverAnswer){
        System.out.println(String.format(
                "\n\tMessage sent: %s" +
                "\n\tServer response: %s" +
                "\n\tsuccess",
                messageSent, serverAnswer));
    }

    public static void printWithHighlight(String message){
        System.out.println("================================================");
        System.out.println(message);
        System.out.println("================================================");
    }

    private static void sendMessage(String msg, PrintWriter out){
        out.println(msg);
        out.flush();
    }

    /**
     * Este método ejecuta 'mini-tests' sobre un único comando. Para ello, envía un mensaje al
     * servidor, almacena su respuesta, y la compara con la respuesta esperada. Si coinciden se
     * pasa el test, sino salta el assert correspondiente.
     *
     * @param messageSent Mensaje enviado al servidor.
     * @param expectedAnswer Respuesta esperada (definidas en la clase ResultsSpec)
     * @param out PrintWriter para envíar mensaje al servidor.
     * @param in BufferedReader para leer respuesta del servidor.
     * @param verbose booleano que si es verdadero hace que se imprima la traza del test con el
     *                método printTestTrace().
     */
    public static void testCommand(String messageSent, String expectedAnswer,
                                   PrintWriter out, BufferedReader in, boolean verbose) throws IOException
    {
        String command = "unrecognized";
        String fields[] = messageSent.split(" ");
        if(fields.length > 1)
            command = fields[1];
        else
            assert 1==2 : "Wrong messageSent by test. Review test method testCommand and arguments.";
        System.out.print(String.format("-Testing command %s...", command));

        sendMessage(messageSent, out);
        String serverAnswer = in.readLine().trim();
        assert serverAnswer.equals(expectedAnswer) : buildErrorMessage(command, messageSent, expectedAnswer, serverAnswer);

        if(verbose)
            printTestTrace(messageSent, serverAnswer);
        else
            System.out.println("success");

    }

    /**
     * 1. Elimina la base de datos y la vuelve a crear, para partir de situación incial.
     * 2. Crea 2 usuarios, registra uno como infected, añade cada usuario una posición y ejecuta listAlarms.
     * Aunque en teoría este test es para comprobar que listAlarms va bien, también comprueba todos los
     * comandos previos necesarios, como addPosition, addUser, login, logout, infected, etc.
     *
     * @param verbose Si true, muestra la traza de todos los métodos testCommand.
     */
    public static void ListAlarmsTEST(boolean verbose) throws IOException, SQLException, InterruptedException {
        //Drop and create sqlite database
        if(deleteFile("Contactus.db"))
            DatabaseCreation.main(new String[] {});
        else{
            return;
        }

        Scanner keyboard = new Scanner(System.in);

        System.out.print("Start the server and press enter: ");
        keyboard.nextLine();

        //server conncetion
        Socket clientSocket = new Socket("localhost", 8000);
        System.out.println("Connected with the server");
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        printWithHighlight("TEST LIST ALARMS");
        Thread.sleep(1000);

        //Start testing

        //add 2 users
        testCommand(addUser(1), addUserSuccess, out, in, verbose);
        testCommand(addUser(2), addUserSuccess, out, in, verbose);

        //infect user 2 and add position 2
        testCommand(login(2), loginSuccess(2, userList[2][0]), out, in, verbose);
        testCommand(infected(), infectedSuccess, out, in, verbose);
        testCommand(addPosition(position2[0],position2[1]), addPositionSuccess, out, in, verbose);
        testCommand(logout(), logoutSuccess, out, in, verbose);

        //login as healthy user 1 and addPosition 1
        testCommand(login(1), loginSuccess(1, userList[1][0]), out, in, verbose);
        testCommand(addPosition(position1[0],position1[1]), addPositionSuccess, out, in, verbose);

        //listAlarms
        testCommand(listAlarms(), listAlarmsSuccess, out, in, verbose);


        printWithHighlight("ALL TEST PASSED!!");
    }


    /**
     * Ejecuta los tests de esta clase (de momento solo ListAlarmsTEST). Para ejecutar esta clase se
     * deben activar los asserts, que por defecto están desactivados. Se pueden activar pasandole como
     * parametro al comando java la opción -ea. Por ejemplo para ejecutar desde la consola:
     * java -cp lib\sqlite-jdbc-3.32.3.2.jar;out\production\contactus\ -ea test.ServerSpec -v
     * (ajustar las rutas de las librerías y de los ficheros .class si fuera necesario). El parametro
     * -v es opcional. Si se pasa el parametro -v (verbose) se muestran más detalles de la traza del test.
     * @param args nada o -v
     */
    public static void main(String[] args) throws IOException, SQLException, InterruptedException {
        boolean verbose = false;
        if(args.length > 0 && args[0].equals("-v"))
            verbose = true;
        ListAlarmsTEST(verbose);

    }





}
