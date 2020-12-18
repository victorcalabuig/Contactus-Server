package server;

import java.util.ArrayList;

import java.util.List;
import java.util.ArrayList;

/**
 * Clase para empaquetar los elementos de una alarma, que son los dos usuarios involucrados
 * y la distancia entre ellos. Se emite una alarma cuando se produce alguna combinación que
 * cumpla la siguiente condición.
 * (healthy | suspect) + (suspect | infected) = alarma
 * En otras palabras, se emiten alarmas para los usuarios healthy y suspect cuando se les
 * acerca un usuario infected o suspect.
 */
public class Alarm {
    int healthyUserId;
    int dangerUserId;
    double distance;

    /**
     * Crea una nueva alarma entre un usuario healthy y un usuario o bien suspect o
     * bien infected.
     * @param healthyUserLocation Location del usuario healthy en el momento de la alarma
     * @param DangerUserLocation Location del usuario infectado/suspect
     */
    public Alarm(Location healthyUserLocation, Location DangerUserLocation){
        healthyUserId = healthyUserLocation.userId;
        dangerUserId = DangerUserLocation.userId;
        distance = healthyUserLocation.distanceToDangerLocation;
    }

    //Constructor only used for debugging
    public Alarm(int dangerUserId, double distance){
        this.dangerUserId = dangerUserId;
        this.distance = distance;
    }

    /**
     * Utilizado para mostrar las alarmas a un usuario (listAlarms ejecutado por usuario
     * normal). Muestra el id del usuario peligroso y la distancia.
     */
    @Override
    public String toString(){
        return String.format("(Danger user %d at %.3f distance)", dangerUserId, distance);
    }

    /**
     * Utilizado para mostrar las alarmas a un usuario admin, ya que además de mostrar
     * el id del usuario peligroso y la distancia, también muestra el id del usuario
     * healhty.
     */
    public String toStringAdmin(){
        return String.format("(%d, %d; %.3f)", healthyUserId, dangerUserId, distance);
    }

    //debugging
    public static void main(String[] args){
        Alarm a1 = new Alarm(1, 1.3);
        Alarm a2 = new Alarm(2, 0.1);
        List<Alarm> alarmList = new ArrayList();
        //alarmList.add(a1);
        //alarmList.add(a2);
        System.out.println(alarmList);
    }

}
