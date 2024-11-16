import java.io.*;
import java.net.*;
import java.util.*;
//import javafx.util.*;

//public class Pair<K,V>;

public class MyServer {
  private static HashMap<String, Pair<String, String>> clientMap = new HashMap<>();

  public static void main (String[] args){
    try{
      Scanner portNum = new Scanner(System.in);
      System.out.println("Enter the Port Number for this server: ");
      int portNum_int = portNum.nextInt();
      System.out.println("The server with Port Number " + portNum_int + " has started!");

      ServerSocket ss = new ServerSocket(portNum_int);
      // HANDSHAKE PROCESS

      while (true){
        try {
          Socket socket = ss.accept();
          handleClient(socket);
        } catch (IOException e){
          System.out.println(e);
        }
      }

      // Socket s = ss.accept();  // THIS CODE FOR ESTABLISH CONNECT BETWEEN TRACKER AND CLIENT
      // DataInputStream dis = new DataInputStream(s.getInputStream());
      // String client_connect = (String)dis.readUTF();
      // System.out.println(client_connect);
      //ss.close();      
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  private static void handleClient(Socket s){
    try (DataInputStream dis = new DataInputStream(s.getInputStream())){
      String clientIP = s.getInetAddress().getHostAddress(); 
      int clientPort = s.getPort(); 
      String key = clientIP + ":" + clientPort; // Store initial client information in the HashMap 
      clientMap.put(key, new Pair<String,String>(clientIP, Integer.toString(clientPort)));

      while (true){
        String clientMessage = dis.readUTF();
        printClientMap();
        if (clientMessage.equalsIgnoreCase("ALIVE")){
          System.out.println("Client with IP address " + s.getInetAddress().getHostAddress() + " is still connect!\n");
        } else {
          System.out.println("Received message from client " + s.getInetAddress().getHostAddress() + ": " +clientMessage);
        }
      }
    } catch (EOFException e){
      System.out.println("Client disconnected!");
    } catch (IOException e){
      System.out.println(e);
    }
  }

  public static void printClientMap(){
    for (Map.Entry<String, Pair<String,String>> entry : clientMap.entrySet()){
      String key = entry.getKey();
      Pair<String,String> tmp = entry.getValue();
      String pairKey = tmp.getKey();
      String pairValue = tmp.getValue();
      System.out.println("\"" + key.split(":")[0] + "\"->\"" + key.split(":")[1] + "\"->\"" + pairValue + "\"->\"" + pairKey);
    }
  }

  public static class Pair<K, V> {
    private K key;
    private V value;
  
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }
  
    public K getKey() {
        return key;
    }
  
    public V getValue() {
        return value;
    }
  
    public void setKey(K key) {
        this.key = key;
    }
  
    public void setValue(V value) {
        this.value = value;
    }
  }

}

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            String clientIP = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();
            String key = clientIP + ":" + clientPort;

            // Store initial client information in the HashMap
            MyServer.addClient(key, new MyServer.Pair<>(clientIP, Integer.toString(clientPort)));

            while (true) {
                String clientMessage = dis.readUTF();
                MyServer.printClientMap();
                if (clientMessage.equalsIgnoreCase("ALIVE")) {
                    System.out.println("Client with IP address " + socket.getInetAddress().getHostAddress() + " is still connected!\n");
                } else {
                    System.out.println("Received message from client " + socket.getInetAddress().getHostAddress() + ": " + clientMessage);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected!");
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}




