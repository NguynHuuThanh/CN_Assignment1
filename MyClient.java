import java.io.*;
import java.net.*;
import java.util.*;

public class MyClient{
  public static void main(String[] args){
    Scanner objRead = new Scanner(System.in);
    String serverAddress;
    int serverPort;
    int clientPort;
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
      String connectMessage = "Client with portNumber " + clientPort + " connected!\n";
      //output.write(connectMessage.getBytes(StandardCharsets.UTF_8));
      output.writeUTF(connectMessage);

      new Thread(new Menu(socket, output, input)).start();

      while (true){
        // Thread.sleep(5000);
        // output.writeUTF("ALIVE");
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

  public Menu(Socket socket, DataOutputStream dis, DataInputStream input){
    this.socket=socket;
    this.dis=dis;
    this.input=input;
  }

  @Override
  public void run() {
    Scanner scanner = new Scanner(System.in);
    boolean running = true;
    // try (DataOutputStream dis = new DataOutputStream(socket.getOutputStream());
    //      DataInputStream input = new DataInputStream(socket.getInputStream())){
    //       this.dis=dis;
    //       this.input=input;
    //      }
    // catch (Exception e){
    //   System.out.println(e);
    // }

    while (running) {
        System.out.println("\nChoose operation:");
        System.out.println("[1] Print file.");
        System.out.println("[2] Download file.");
        System.out.println("[3] Exit");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume the newline

        switch (choice) {
            case 1:
                //System.out.println("Enter the path of the file to upload: ");
                //String infoHashUpload = scanner.nextLine();
                //handleUpload(infoHashUpload);
                // try (DataOutputStream dis = new DataOutputStream(socket.getOutputStream()))
                // {dis.writeUTF("PRINT!");}
                // catch (Exception e) {System.out.println(e);}
                // break;
                try {
                  dis.writeUTF("PRINT!");
                  String print = input.readUTF();
                  System.out.println(print);
                  System.out.println("haha");
                  break;
                } catch (Exception e) {
                  System.out.println(e);
                }
                break;

            case 2:
                System.out.println("Enter the info_hash of the file to download: ");
                String infoHashDownload = scanner.nextLine();
                // how to get the number of pieces in this by socket
                //handleDownload(infoHashDownload);
                //break;
                // Create a new thread for the download process
                Thread downloadThread = new Thread(() -> {
                  try {
                      handleDownload(infoHashDownload);
                  } catch (Exception e) {
                      System.out.println("Error generate a new thread for file download: " + e.getMessage());
                  }
              });
              downloadThread.start();
              break;

            case 3:
                System.out.println("Exiting...");
                running = false;
                if (socket != null && !socket.isClosed()) {
                    try {
                        //socket.close();
                        //System.exit(0);
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
  private void handleUpload(String filePath) {}

  @SuppressWarnings("unchecked")
  private void handleDownload(String infoHash) {
      try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
          DataInputStream in = new DataInputStream(socket.getInputStream())) {

          // Request piece-to-peer mapping from server
          out.writeUTF("Finding file:" + infoHash);
          ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
          Object response = ois.readObject();

          if (response instanceof HashMap) {
              HashMap<Integer, ArrayList<String>> pieceMap = (HashMap<Integer, ArrayList<String>>) response;
              System.out.println("Received piece-to-peer mapping for file: " + infoHash);

              String downloadDirectory = "downloads/" + infoHash;
              File dir = new File(downloadDirectory);
              if (!dir.exists()) dir.mkdirs();

              // Download pieces from peers
              for (Map.Entry<Integer, ArrayList<String>> entry : pieceMap.entrySet()) {
                  int pieceIndex = entry.getKey();
                  ArrayList<String> peers = entry.getValue();

                  for (String peer : peers) {
                      String[] peerInfo = peer.split(":");
                      String peerIP = peerInfo[0];
                      int peerPort = Integer.parseInt(peerInfo[1]);

                      if (downloadPiece(peerIP, peerPort, infoHash, pieceIndex, downloadDirectory)) {
                          break; // Move to the next piece after successful download
                      }
                  }
              }

              // Reassemble the file
              reassembleFile(downloadDirectory, "final_file_" + infoHash);
              System.out.println("File downloaded and reassembled successfully!");
          } else {
              System.out.println(response.toString());
          }
      } catch (Exception e) {
          e.printStackTrace();
      }
  }



private boolean downloadPiece(String peerIP, int peerPort, String infoHash, int pieceIndex, String downloadDirectory) {
  try (Socket peerSocket = new Socket(peerIP, peerPort);
       DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
       DataInputStream in = new DataInputStream(peerSocket.getInputStream())) {

      // Request the specific piece
      out.writeUTF("REQUEST_PIECE:" + infoHash + ":" + pieceIndex);

      // Receive the piece data
      String response = in.readUTF();
      if (response.equals("PIECE_FOUND")) {
          File pieceFile = new File(downloadDirectory, String.valueOf(pieceIndex));
          try (FileOutputStream fos = new FileOutputStream(pieceFile)) {
              byte[] buffer = new byte[1024];
              int bytesRead;
              while ((bytesRead = in.read(buffer)) != -1) {
                  fos.write(buffer, 0, bytesRead);
              }
          }
          System.out.println("Downloaded piece " + pieceIndex + " from peer " + peerIP + ":" + peerPort);
          return true;
      } else {
          System.out.println("Peer " + peerIP + ":" + peerPort + " does not have piece " + pieceIndex);
      }
  } catch (IOException e) {
      System.out.println("Failed to download piece " + pieceIndex + " from peer " + peerIP + ":" + peerPort);
  }
  return false;
}

private void reassembleFile(String piecesDirectory, String outputFile) {
  try {
      File dir = new File(piecesDirectory);
      File[] pieces = dir.listFiles((d, name) -> name.matches("\\d+")); // Match files named as numbers

      if (pieces == null || pieces.length == 0) {
          System.out.println("No pieces found to reassemble.");
          return;
      }

      Arrays.sort(pieces, Comparator.comparingInt(f -> Integer.parseInt(f.getName())));

      try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
          for (File piece : pieces) {
              try (FileInputStream inputStream = new FileInputStream(piece)) {
                  byte[] buffer = new byte[1024];
                  int bytesRead;
                  while ((bytesRead = inputStream.read(buffer)) != -1) {
                      outputStream.write(buffer, 0, bytesRead);
                  }
              }
          }
      }
      System.out.println("File reassembled successfully: " + outputFile);
  } catch (IOException e) {
      e.printStackTrace();
  }
}


}



class Download_pieces implements Runnable {
  private int numPieces;
  private String info_hash;

  public Download_pieces(int numPieces, String info_hash){
    this.numPieces=numPieces;
    this.info_hash=info_hash;
  }
  @Override
  public void run(){
    
  }

}