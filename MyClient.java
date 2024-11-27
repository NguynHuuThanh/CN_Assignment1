import java.io.*;
import java.net.*;
import java.util.*;

public class MyClient{
  public static int clientPort;
  public static void main(String[] args){
      

    Scanner objRead = new Scanner(System.in);
    String serverAddress;
    int serverPort;
    //int clientPort;
    //this.myserver = new MyServer();

    // System.out.println("Enter the Server Address: ");
    // serverAddress = objRead.nextLine();
    
    // System.out.println("Enter the Port Number of server: ");
    // serverPort = objRead.nextInt();

    System.out.println("Enter the Port Number for this client: ");
    clientPort = objRead.nextInt();

    try (Socket socket = new Socket()){
      
      socket.bind(new InetSocketAddress(clientPort));
      //socket.connect(new InetSocketAddress(serverAddress,serverPort));
      socket.connect(new InetSocketAddress("localhost",9000));

      String connectSuccess = "Connect successfully!\n";
      System.out.println(connectSuccess);
      
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      DataInputStream input = new DataInputStream(socket.getInputStream());
      //ObjectInputStream ois = new ObjectInputStream(input);
      String connectMessage = "Client with portNumber " + clientPort + " connected!\n";
      //output.write(connectMessage.getBytes(StandardCharsets.UTF_8));
      try {output.writeUTF(connectMessage);}
      catch (Exception e) {System.out.println(e);}

      ServerSocket clientSS = new ServerSocket(clientPort);

      new Thread(new Menu(socket, output, input)).start();
      new Thread(new Hankshake(clientSS)).start();

      while (true){

      }

    } catch (java.net.SocketException e) {
      System.out.println(e);
      System.out.println("Client disconnected!");
    }
    catch(Exception e){
      System.out.println(e);
    }
  }
}

class Menu implements Runnable {
  private final Socket socket;
  private DataOutputStream dis;
  private DataInputStream input;
  //private ObjectInputStream ois;

  public Menu(Socket socket, DataOutputStream dis, DataInputStream input){
    this.socket=socket;
    this.dis=dis;
    this.input=input;
  }

  @Override
  public void run() {
    Scanner scanner = new Scanner(System.in);
    boolean running = true;

    while (running) {
        System.out.println("\nChoose operation:");
        System.out.println("[1] Print file.");
        System.out.println("[2] Download file.");
        System.out.println("[3] Exit");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume the newline

        switch (choice) {
            case 1:

                try {
                  dis.writeUTF("PRINT!");
                  System.out.println(input.readUTF());
                  break;
                } catch (Exception e) {
                  System.out.println(e);
                }
                break;

                case 2:
              PrintStream originalOut = System.out; // Save the original System.out
              PrintStream originalErr = System.err; // Save the original System.err
              try {
                  

                  // Start logging
                  System.out.println("Logging started at: " + new Date());
                  System.out.println("Enter the Name of the file to download: ");

                  // Redirect System.out and System.err to a log file
                  FileOutputStream fileOutputStream = new FileOutputStream(Integer.toString(MyClient.clientPort) + ".log", true);
                  PrintStream logStream = new PrintStream(fileOutputStream);
                  System.setOut(logStream);
                  System.setErr(logStream);

                  String infoHashDownload = scanner.nextLine();
                  dis.writeUTF("Finding file:" + infoHashDownload);
                  String tmp = "";

                  try {
                      tmp = input.readUTF();
                  } catch (Exception e) {
                      System.out.println("Error reading response: " + e.getMessage());
                  }

                  if (tmp.contains("File not found on the server.")) {
                      System.out.println("File not found on the server.");
                      break;
                  }

                  int num_piece = Integer.parseInt(tmp);

                  // Define the buffer to store all the pieces
                  byte[][] buffer = new byte[num_piece][512000]; // Each piece is a byte array
                  boolean check = false;

                  while (true) {
                      try {
                          tmp = input.readUTF();
                      } catch (Exception e) {
                          System.out.println("Error during communication: " + e.getMessage());
                      }

                      if (tmp.contains("DONE")) {
                          if (check) {
                              System.out.println("All pieces received.");
                          }
                          break;
                      }

                      String piece = tmp.split(";")[0];
                      String peerIP = tmp.split(";")[1];
                      String peerDir = tmp.split(";")[2];
                      String infoHash = tmp.split(";")[3];

                      if (Integer.valueOf(peerIP.split(":")[1]) == MyClient.clientPort) {
                          System.out.println("You already have this file!");
                          check = true;
                          break;
                      }

                      try {
                          int pieceIndex = Integer.valueOf(piece);
                          boolean success = downloadPiece(peerIP.split(":")[0],
                                                          Integer.valueOf(peerIP.split(":")[1]),
                                                          infoHash, 
                                                          pieceIndex, 
                                                          peerDir, 
                                                          infoHashDownload, 
                                                          buffer);

                          if (!success) {
                              check = true;
                              System.out.println("Failed to download piece " + pieceIndex);
                          }

                      } catch (Exception e) {
                          check = true;
                          System.out.println("Error downloading piece " + piece + ": " + e.getMessage());
                      }
                  }

                  if (!check) {
                      System.out.println("Reassembling the file...");
                      reassembleFile(MyServer.BASE_PATH + Integer.toString(MyClient.clientPort) + "/" + infoHashDownload, buffer);
                  }
              } catch (Exception e) {
                  System.out.println("Error: " + e.getMessage());
              } finally {
                  // Restore the original System.out and System.err
                  System.setOut(originalOut);
                  System.setErr(originalErr);

                  // Log restoration
                  System.out.println("Logging ended. Output restored to console.");
              }
              break;

            
                

            case 3:
                System.out.println("Exiting...");
                running = false;
                if (socket != null && !socket.isClosed()) {
                    try {

                    } catch (Exception e) {//catch (IOException e) 
                        //e.printStackTrace();
                        System.out.println(e);
                    }
                }
                System.exit(0);
                break;

            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }
  }

  private boolean downloadPiece(String peerIP, int peerPort, String infoHash, int pieceIndex, 
                              String downloadDirectory, String nameFile, byte[][] buffer) {
    try (Socket peerSocket = new Socket(peerIP, peerPort);
         DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
         DataInputStream in = new DataInputStream(peerSocket.getInputStream())) {

        // Send handshake and piece request
        out.writeUTF("Handshake sent from " + this.socket.getInetAddress() + ":" + this.socket.getLocalPort());
        System.out.println("Handshake sent from " + this.socket.getInetAddress() + ":" + this.socket.getLocalPort());
        System.out.println(in.readUTF());
        out.writeUTF(downloadDirectory + "/" + nameFile); // File name/path
        out.writeUTF(String.valueOf(pieceIndex));            // Requested piece index

        InputStream input = peerSocket.getInputStream();

        // Buffer to hold the piece
        byte[] pieceBuffer = new byte[512000]; // Adjust size to your protocol's piece size
        int bytesRead, totalBytesRead = 0;

        // Download piece into the temporary buffer
        System.out.println("Downloading piece " + pieceIndex + "!");
        while ((bytesRead = input.read(pieceBuffer, totalBytesRead, pieceBuffer.length - totalBytesRead)) > 0) {
            totalBytesRead += bytesRead;

            //System.out.println("Received " + bytesRead + " bytes. Total: " + totalBytesRead + " bytes.");
            if (totalBytesRead == pieceBuffer.length) {
                break; // Stop if the piece is fully read
            }

        }

        if (totalBytesRead > 0) {
            // Save the downloaded piece to the shared buffer in the correct position
            buffer[pieceIndex] = Arrays.copyOf(pieceBuffer, totalBytesRead);
            System.out.println("Piece " + pieceIndex + " stored in buffer.");
        } else {
            System.out.println("No data received for piece " + pieceIndex);
            return false; // Failure
        }

        //out.writeUTF("Done");
        return true; // Success

    } catch (IOException e) {
        e.printStackTrace();
        return false; // Failure
    }
}

private void reassembleFile(String outputFile, byte[][] buffer_pieces) {
  try {
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
        // Iterate through each piece in buffer_pieces
        for (byte[] piece : buffer_pieces) {
            outputStream.write(piece); // Write the piece to the output file
        }
    }
    System.out.println("File reassembled successfully: " + outputFile);
  } catch (IOException e) {
      e.printStackTrace();
  }
}


}

class Hankshake implements Runnable {
  private ServerSocket socket;
  //private String peerID;
  public Hankshake(ServerSocket socket){
    this.socket=socket;
  }
  @Override
  public void run() {
    Scanner scanner = new Scanner(System.in);
    boolean running = true;

    while (true) {
      try (Socket client = this.socket.accept();
           DataInputStream input = new DataInputStream(client.getInputStream());
           DataOutputStream output = new DataOutputStream(client.getOutputStream());
           OutputStream outputStream = client.getOutputStream()) {

          System.out.println(input.readUTF());
          output.writeUTF("Peer " + this.socket.getInetAddress().getHostAddress() + ":" + this.socket.getLocalPort() + " received!");
          String path = input.readUTF();
          int pieceIndex = Integer.parseInt(input.readUTF());
          
          //System.out.println("hohohohoh");
          //System.out.println(path);
          try (FileInputStream fileInputStream = new FileInputStream(path)) {
              byte[] buffer = new byte[512000];
              int start = pieceIndex * 512000;
              fileInputStream.skip(start); // Start from the correct position

              int bytesRead;
              //System.out.println("check sent 2");
              while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                //System.out.println("check sent 3");
                  outputStream.write(buffer, 0, bytesRead); 
                  //System.out.println("Sent " + bytesRead + " bytes for piece " + pieceIndex);

              }
              //socket.close();
          }
          
      } catch (IOException e) {
          //System.out.println("Error handling client: " + e.getMessage());
      }
    }
    }
  }

  