import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MyServer {
    private static HashMap<String, ArrayList<HashMap<String, ArrayList<String>>>> clientMap = new HashMap<>();
    public static final String BASE_PATH = "D:" + File.separator + "Tamnd" + File.separator + 
                                          "Tailieudh" + File.separator + "Computer network" + 
                                          File.separator + "Project" + File.separator;

    public static void main(String[] args) {
        try {
            Scanner portNum = new Scanner(System.in);
            System.out.println("Enter the Port Number for this server: ");
            int portNum_int = portNum.nextInt();
            System.out.println("The server with Port Number " + portNum_int + " has started!");

            ServerSocket ss = new ServerSocket(portNum_int);

            while (true) {
                try {
                    Socket socket = ss.accept();
                    new Thread(new ClientHandler(socket)).start();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void addClient(String key, ArrayList<HashMap<String, ArrayList<String>>> clientInfo) {
        clientMap.put(key, clientInfo);
    }

    public static String calculateInfoHash(MetaInfo.InfoDictionary info) {
        try {
            BEncoder encoder = new BEncoder();
            encoder.write(info.toBEncodedDictionary());
            String encodedInfo = encoder.toString();
            
            byte[] hash = MetaInfo.calculatePieceHash(encodedInfo.getBytes(StandardCharsets.UTF_8));
            
            return URLEncoder.encode(new String(hash, StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createTorrentFile(File sourceFile, String clientPath, int clientPort) throws IOException, NoSuchAlgorithmException {
        MetaInfo metaInfo = new MetaInfo();
        
        metaInfo.setAnnounce("http://localhost:" + clientPort);
        metaInfo.setInfo(sourceFile.getName(), 512000); // 512KB piece length
        
        metaInfo.setSingleFile(sourceFile.length());
        MetaInfo.setFilePieces(metaInfo, sourceFile);
        
        String torrentPath = clientPath + File.separator + sourceFile.getName() + ".torrent";
        metaInfo.saveToFile(torrentPath);
    }

    public static void processClientFolder(String clientPath, String clientKey, int clientPort) {
        File folder = new File(clientPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        ArrayList<HashMap<String, ArrayList<String>>> filesList = new ArrayList<>();
        File[] files = folder.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().endsWith(".torrent")) {
                    try {
                        File torrentFile = new File(clientPath + File.separator + file.getName() + ".torrent");
                        if (!torrentFile.exists()) {
                            createTorrentFile(file, clientPath, clientPort);
                        }
                        
                        MetaInfo metaInfo = MetaInfo.loadFromFile(torrentFile.getPath());
                        String infoHash = calculateInfoHash(metaInfo.getInfo());
                        
                        // Create file info
                        HashMap<String, ArrayList<String>> fileInfo = new HashMap<>();
                        ArrayList<String> metaInfoData = new ArrayList<>();
                        metaInfoData.add(file.getName());
                        metaInfoData.add(String.valueOf(file.length()));
                        metaInfoData.add(metaInfo.encode());
                        
                        fileInfo.put(infoHash, metaInfoData);
                        filesList.add(fileInfo);
                        
                    } catch (Exception e) {
                        System.out.println("Error processing file: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        
        addClient(clientKey, filesList);
    }

    public static void printClientMap() {
        for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<String>>>> entry : clientMap.entrySet()) {
            String key = entry.getKey();
            ArrayList<HashMap<String, ArrayList<String>>> list_file = entry.getValue();
            
            System.out.println("\nClient: " + key);
            if (list_file.isEmpty()){
                String[] parts = key.split(":");
                System.out.println("Client " + parts[1] + " has no file to show!");
                continue;
            }
            else{
                for (HashMap<String, ArrayList<String>> fileInfo : list_file) {
                    for (Map.Entry<String, ArrayList<String>> info : fileInfo.entrySet()) {
                        System.out.println("Info Hash: " + info.getKey());
                        ArrayList<String> fileData = info.getValue();
                        System.out.println("  File Name: " + fileData.get(0));
                        System.out.println("  File Size: " + fileData.get(1) + " bytes");
                    }
                }
            }
            System.out.println();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    public int clientPort;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            String clientIP = socket.getInetAddress().getHostAddress();
            clientPort = socket.getPort();
            String key = clientIP + ":" + clientPort;

            // Create client-specific folder path using port number
            String clientPath = MyServer.BASE_PATH + clientPort;
            
            // Process client folder and create torrent files
            MyServer.processClientFolder(clientPath, key, clientPort);

            while (true) {
                String clientMessage = dis.readUTF();
                MyServer.printClientMap();
                if (clientMessage.equalsIgnoreCase("ALIVE")) {
                    System.out.println("Client with IP address " + socket.getInetAddress().getHostAddress() + " and Port: " + clientPort + " is still connected!\n");
                } else {
                    System.out.println("Received message from client " + clientPort + ": " + clientMessage);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client " + clientPort + " disconnected!");
        } catch (IOException e) {
            System.out.println("Client " + clientPort + " disconnected!");
            shutdown();
        }
    }

    public void shutdown() {
        try{
         if(!socket.isClosed()){
            socket.close();
         }

        }catch (IOException e){
            e.printStackTrace();
        }
     }
}