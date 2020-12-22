package server;

import java.util.ArrayList;
import java.util.List;

public class User {
    //Campos de un user genericos
    public int userId;
    public String username;
    public String email;
    public String password;
    public Boolean state = false; //default is healthy (in sqlite is 0)
    public Location lastLocation;



    public User(){

    }

    public User(int userId, String username){
        this.userId = userId;
        this.username = username;
    }

    public String toString(){
        //"%-25s%-5s%-15s%-5s%-15s\n", "Time", "|", "Latitude", "|", "Longitude");
        return String.format("%d-%s", userId, username);
    }

    public static void main(String[] args){
        User usr = new User(1,"admin");
        User usr2 = new User(2, "vic");
        List<User> lst = new ArrayList();
        lst.add(usr);
        lst.add(usr2);
        StringBuilder usersSB = new StringBuilder();
        for(User user : lst){
            usersSB.append(user + "\n");
        }

        System.out.println(usersSB);
    }

}


