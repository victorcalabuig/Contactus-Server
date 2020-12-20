package server;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Clase para encapsular una Location de cara ha realizar los algoritmos de suspect e infected.
 */
public class Location implements Comparable<Location> {

    /**
     * Por debajo de esta distancia se dispara una alarma.
     */
    public static double MIN_DISTANCE = 2;

    /**
     * Tiempo en milisegundos que consideramos para determinar si 2 personas se han cruzado.
     * Si 2 personas tienen posiciones en el mismo sitio pero la diferencia de fecha-hora de
     * esas 2 posiciones es superior a MAX_TIME_DIFF, no se considera que esas 2 personas se
     * hayan cruzado.
     * No confundir con el tiempo necesario para que se considere a un usuario healthy como
     * contacto cercano (y por lo tanto pase a suspect).
     */
    public static long MAX_TIME_DIFF = 60000;

    //Campos de la tabla location
    long locationId;
    int userId;
    long time;
    double latitude;
    double longitude;

    //atributo adicional para mejorar rendimiento
    String username;
    Double distanceToDangerLocation;

    /**
     * Construye una nueva Location a partir del resultado de una consulta, que se pasa por
     * el ResultSET locationRS.
     * @param locationRS ResultSet que apunta a una fila de la tabla Location.
     */
    public Location(ResultSet locationRS) throws SQLException {
        locationId = locationRS.getLong(1);
        userId = locationRS.getInt(2);
        time = locationRS.getLong(3);

        //sqlite guarda decimales usando la coma como separador, por lo que hay que hacer esta transformación
        String latitudeString = locationRS.getString(4).replaceAll(",",".");
        String longitudeString = locationRS.getString(5).replaceAll(",",".");

        latitude = Double.parseDouble(latitudeString);
        longitude = Double.parseDouble(longitudeString);
    }

    /**
     * Constructor utilizado para eliminar una Location de una lista de Locations.
     */
    public Location(long locationId){
        this.locationId = locationId;
    }

    /**
     * Construye una Location solo con las coordenadas.
     */
    public Location(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Comprueba si 2 Locations se encuentran en el mismo sitio en el mismo momento.
     * @return True si las 2 Locations han coincidido, false sino.
     */
    public boolean isNear(Location dangerLocation) {
        if(calculateTimeDiff(this, dangerLocation) > MAX_TIME_DIFF) return false;
        this.distanceToDangerLocation = calculateDistance(this, dangerLocation);
        if(this.distanceToDangerLocation > MIN_DISTANCE) return false;
        return true;
    }

    /**
     * Utiliza el teorema de Pitágoras (a^2 + b^2 = c^2) para hallar la distancia entre 2 puntos.
     * @param l1 Objeto Location que envuelve la primera posición.
     * @param l2 Objeto Location que envuelve la segunda posición.
     * @return distancia entre los dos puntos
     */
    public static double calculateDistance(Location l1, Location l2){
        double latitudeDif = Math.abs(l1.latitude - l2.latitude);
        double longitudeDif = Math.abs(l1.longitude - l2.longitude);
        return Math.sqrt(Math.pow(latitudeDif,2) + Math.pow(longitudeDif,2));
    }

    /**
     * Calcula la diferencia temporal entre 2 objetos Location utilizando el campo time.
     * @return Diferencia temporal en milisegundos.
     */
    public static long calculateTimeDiff(Location l1, Location l2){
        return Math.abs(l1.time - l2.time);
    }

    /**
     * Comprueba si 2 posiciones son iguales. Dos posiciones son iguales si tiene el mismo
     * locationId.
     */
    @Override
    public boolean equals(Object o){
        Location l2 = (Location) o;
        return this.locationId == ((Location) o).locationId;
    }

    /**
     * Compara 2 Locations según su fecha-hora. Este método lo utilizará internamente Java
     * para ordenar las posiciones por fecha en algunos algoritmos.
     */
    @Override
    public int compareTo(Location l2){
        return Long.compare(this.time, l2.time);
    }

    @Override
    public String toString(){
        return userId + "(" + latitude + ", " + longitude + ")";
    }

    public static void main(String[] args){
        String num = "1,2";
        Double d = Double.valueOf(num);
        System.out.println(d);
    }

}
