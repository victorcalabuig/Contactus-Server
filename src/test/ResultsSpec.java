package test;

import server.Location;
import server.Alarm;

/**
 * La idea de esta clase es guardar varios elementos (variables y métodos) comunes a varios
 * tests y que luego las clases de test extiendan esta para utilizarlo.
 */
public class ResultsSpec {

    public static String[][] userList = {
            {"admin", "admin"},
            {"vic", "123"},
            {"pep", "123"}
    };

    //Esta son las posiciones añadidas por los usuarios y sobre las que se calcula una posible alarma
    //Se pueden modificar para ver que se añade la alarma cuando toca y no se añade cuando no toca.
    public static String[] position1 = {"0.0", "0.0"}; //usuario 1 (healthy)
    public static String[] position2=  {"1.0", "1.0"}; //usuario 2 (infected)

    public static Location location1 = new Location(
            Double.parseDouble(position1[0]),
            Double.parseDouble(position1[1]));

    public static Location location2 = new Location(
            Double.parseDouble(position2[0]),
            Double.parseDouble(position2[1]));

    public static double distanceBetweenPosition1And2 = Location.calculateDistance(location1, location2);

    static String addUserSuccess = "addUser 0";
    static String addPositionSuccess = "addPosition 0";
    static String infectedSuccess = "infected 0";
    static String logoutSuccess = "logout 0";
    static String listAlarmsSuccess = buildAlarmsSuccess();

    public static String loginSuccess(int userId, String username){
        return String.format("login 0 %s %s", (userId+1), username);
    }

    public static String buildAlarmsSuccess(){
        StringBuilder serverResponse = new StringBuilder();
        serverResponse.append("listAlarms 0");
        if(distanceBetweenPosition1And2 < Location.MIN_DISTANCE){
            Alarm alarm = new Alarm(3, distanceBetweenPosition1And2);
            serverResponse.append(String.format(" 1 Alarm detected: [%s]", alarm));
        }
        return serverResponse.toString();
    }

    /* //To uncomment this method you first have to comment main method in ServerSpec.java class.
    public static void main(String[] args){
        System.out.println(listAlarmsSuccess);
    }
    */
}
