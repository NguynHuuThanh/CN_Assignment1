import java.io.*;
import java.net.*;
import java.util.*;

public class MyClient{
  public static void main(String[] args){
    Scanner objRead = new Scanner(System.in);
    String serverAddress;
    int serverPort;
    int clientPort;

    System.out.println("Enter the Server Address: ");
    serverAddress = objRead.nextLine();
    
    System.out.println("Enter the Port Number of server: ");
    serverPort = objRead.nextInt();

    System.out.println("Enter the Port Number for this client: ");
    clientPort = objRead.nextInt();

    try (Socket socket = new Socket()){
      
      socket.bind(new InetSocketAddress(clientPort));
      socket.connect(new InetSocketAddress(serverAddress,serverPort));

      String connectSuccess = "Connect successfully!\n";
      System.out.println(connectSuccess);
      
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      String connectMessage = "Client with portNumber " + clientPort + " connected!\n";
      //output.write(connectMessage.getBytes(StandardCharsets.UTF_8));
      output.writeUTF(connectMessage);

      while (true){
        Thread.sleep(5000);
        output.writeUTF("ALIVE");
      }

    } catch (java.net.SocketException e) {
      System.out.println("Client disconnected!");
    }
    catch(Exception e){
      System.out.println(e);
    }
  }
}