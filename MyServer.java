import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MyServer {
    public static HashMap<String, HashMap<Integer, ArrayList<String>>> file_list = new HashMap<>();
    
    public static HashMap<String, HashMap<String, HashMap<Integer, ArrayList<String>>>> file_list2 = new HashMap<>();
    
    //public static final String BASE_PATH = "D:" + File.separator + "Tamnd" + File.separator + "Tailieudh" + File.separator + "Computer network" + File.separator + "Project" + File.separator;
    //public static final String BASE_PATH = File.separator + "Users" + File.separator + "hoangnguyen" + File.separator + "Desktop" + File.separator + "temporary" + File.separator + "CN-Assignment1-main" + File.separator;

    public static final String BASE_PATH = "/Users/hoangnguyen/Desktop/temporary/CN_Assignment1-main/";
    public static void main(String[] args) {
        try {
            Scanner portNum = new Scanner(System.in);
            //System.out.println("Enter the Port Number for this server: ");
            //int portNum_int = portNum.nextInt();
            //System.out.println("The server with Port Number " + portNum_int + " has started!");
            System.out.println("The server with Port Number " + "9000" + " has started!");

            //ServerSocket ss = new ServerSocket(portNum_int);
            ServerSocket ss = new ServerSocket(9000);

            while (true) {
                try {
                    Socket socket = ss.accept();
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                    new Thread(new ClientHandler(socket,output,input)).start();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String calculateInfoHash(MetaInfo.InfoDictionary info) {
        try {
            Map<String, Object> sortedInfoDict = new TreeMap<>();
            Map<String, Object> originalDict = info.toBEncodedDictionary();
            sortedInfoDict.putAll(originalDict);
            
            BEncoder encoder = new BEncoder();
            encoder.write(sortedInfoDict);
            String encodedInfo = encoder.toString();
            
            byte[] hash = MetaInfo.calculatePieceHash(encodedInfo.getBytes(StandardCharsets.ISO_8859_1));
            //String encodedHash = URLEncoder.encode(new String(hash, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1.toString());
            //String decodedHash = URLDecoder.decode(encodedHash, StandardCharsets.ISO_8859_1.toString());
            //byte[] originalHash = decodedHash.getBytes(StandardCharsets.ISO_8859_1);

            //System.out.println("Check if the hash is equal!" + Arrays.equals(hash, originalHash));
            // System.out.print("Byte array contents: ");
            // for (byte b : hash) {
            //     System.out.print(b + " "); // Print each byte as an integer
            // }

            // System.out.print("\n\n\n");
            // System.out.print("Byte array contents: ");
            // for (byte b: originalHash) {
            //     System.out.print(b + " "); // Print each byte as an integer
            // }
            return URLEncoder.encode(new String(hash, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1.toString());
            //String str = new String(hash, StandardCharsets.UTF_8);
            //return str;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createTorrentFile(File source, String clientPath, int clientPort) throws IOException, NoSuchAlgorithmException {
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setAnnounce("http://localhost:" + clientPort);
        metaInfo.setInfo(source.getName(), 512000); // 512KB piece length

        if (source.isDirectory()) {
            List<MetaInfo.InfoDictionary.FileInfo> files = new ArrayList<>();
            processDirectory(source, source, files);
            metaInfo.setMultipleFiles(files);
        } else {
            metaInfo.setSingleFile(source.length());
        }

        MetaInfo.setFilePieces(metaInfo, source);
        String torrentPath = clientPath + File.separator + source.getName() + ".torrent";
        metaInfo.saveToFile(torrentPath);
    }

    private static void processDirectory(File baseDir, File currentDir, List<MetaInfo.InfoDictionary.FileInfo> files) {
        File[] contents = currentDir.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isFile()) {
                    List<String> path = new ArrayList<>();
                    String relativePath = baseDir.toPath().relativize(file.toPath()).toString();
                    String[] pathParts = relativePath.split(File.separator);
                    Collections.addAll(path, pathParts);
                    files.add(new MetaInfo.InfoDictionary.FileInfo(file.length(), path));
                } else if (file.isDirectory()) {
                    processDirectory(baseDir, file, files);
                }
            }
        }
    }

    public static void processClientFiles(String clientPath, String clientKey) {
        File folder = new File(clientPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        
        processFilesRecursively(folder, clientKey);
    }

    private static void processFilesRecursively(File source, String clientKey) {
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().endsWith(".torrent")) {
                    try {
                        File torrentFile = new File(file.getParent(), file.getName() + ".torrent");
                        if (!torrentFile.exists()) {
                            createTorrentFile(file, file.getParent(), Integer.parseInt(clientKey.split(":")[1]));
                        }
    
                        MetaInfo metaInfo = MetaInfo.loadFromFile(torrentFile.getPath());
                        String infoHash = calculateInfoHash(metaInfo.getInfo());
    
                        if (!file_list.containsKey(infoHash)) {
                            file_list.put(infoHash, new HashMap<>());
    
                            // Initialize file_list2 safely
                            file_list2.computeIfAbsent(file.getName(), k -> new HashMap<>())
                                      .computeIfAbsent(infoHash, k -> new HashMap<>());
                        }
    
                        int totalPieces = metaInfo.getInfo().getPieces().length / 20;
    
                        //System.out.println("File name: " + file.getName());
                        //System.out.println("Number of pieces: " + totalPieces);
    
                        // Add client as having all pieces
                        HashMap<Integer, ArrayList<String>> pieceMap = file_list.get(infoHash);
                        HashMap<Integer, ArrayList<String>> pieceMap2 =
                                file_list2.computeIfAbsent(file.getName(), k -> new HashMap<>())
                                          .computeIfAbsent(infoHash, k -> new HashMap<>());
    
                        for (int i = 0; i < totalPieces; i++) {
                        pieceMap.computeIfAbsent(i, k -> new ArrayList<>());
                        pieceMap2.computeIfAbsent(i, k -> new ArrayList<>());
                    
                        if (!pieceMap.get(i).contains(clientKey)) {
                            pieceMap.get(i).add(clientKey);
                        }
                    
                        if (!pieceMap2.get(i).contains(clientKey)) {
                            pieceMap2.get(i).add(clientKey);
                        }
                    }
                                        
                    } catch (Exception e) {
                        System.out.println("Error processing file: " + file.getName());
                        e.printStackTrace();
                    }
                } else if (file.isDirectory()) {
                    File[] contents = file.listFiles();
                    if (contents != null) {
                        processFilesRecursively(file, clientKey);
                    }
                }
            }
        }
    }
    
    public static void printFileList() {
        for (Map.Entry<String, HashMap<Integer, ArrayList<String>>> entry : file_list.entrySet()) {
            String infoHash = entry.getKey();
            HashMap<Integer, ArrayList<String>> pieceMap = entry.getValue();
            System.out.println("\nFile Info Hash: " + infoHash);
            System.out.println("Piece Distribution:");
            
            for (Map.Entry<Integer, ArrayList<String>> pieceEntry : pieceMap.entrySet()) {
                System.out.println("  Piece " + pieceEntry.getKey() + " is available from clients: " + 
                                 String.join(", ", pieceEntry.getValue()));
            }
        }
    }

    public static String printFileList_str() {
        String ans ="";
        for (Map.Entry<String, HashMap<Integer, ArrayList<String>>> entry : file_list.entrySet()) {
            String infoHash = entry.getKey();
            HashMap<Integer, ArrayList<String>> pieceMap = entry.getValue();
            ans = ans + "\nFile Info Hash: " + infoHash + "\nPiece Distribution: ";
            for (Map.Entry<Integer, ArrayList<String>> pieceEntry : pieceMap.entrySet()) {
                ans = ans + "  Piece " + pieceEntry.getKey() + " is available from clients: " + String.join(", ", pieceEntry.getValue()) + "\n";
            }
        }
        return ans;
    }

    // ADD NEW CODE
    public static String PrintListFile(){
        String ans = "\nList of file: \n";
        int i = 1;
        for (Map.Entry<String, HashMap<String, HashMap<Integer, ArrayList<String>>>> entry : file_list2.entrySet()){
            String namefile = entry.getKey();
            HashMap<String, HashMap<Integer, ArrayList<String>>> fullmap = entry.getValue();
            for (Map.Entry<String, HashMap<Integer, ArrayList<String>>> fullmap_entry : fullmap.entrySet()) {    
                String infoHash = fullmap_entry.getKey();
                HashMap<Integer, ArrayList<String>> pieceMap = fullmap_entry.getValue();
                ans = ans + i + ". " + namefile;
                ans = ans + "\nFile Info Hash: " + infoHash;
                ans = ans + "\nPiece Distribution: " + "\n";
                for (Map.Entry<Integer, ArrayList<String>> pieceEntry : pieceMap.entrySet()) {
                    ans = ans + "  Piece " + pieceEntry.getKey() + " is available from clients: " + String.join(", ", pieceEntry.getValue()) + "\n";
                }
                ans += "\n";
                ++i;
            }
        }
        return ans;
    }

    public static Map<Integer, List<String>> getPieceAvailability(String infoHash) {
        return new HashMap<>(file_list.getOrDefault(infoHash, new HashMap<>()));
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    public int clientPort;
    private DataOutputStream output;
    private DataInputStream input;
    private ObjectOutputStream oos;

    public ClientHandler(Socket socket, DataOutputStream output, DataInputStream input, ObjectOutputStream oos) {
        this.socket = socket;
        this.output = output;
        this.input = input;
        this.oos = oos;
    }

    public ClientHandler(Socket socket, DataOutputStream output, DataInputStream input){
        this.socket = socket;
        this.output = output;
        this.input = input;
    }

    @Override
    public void run() {
        //try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
        try {
            String clientIP = socket.getInetAddress().getHostAddress();
            clientPort = socket.getPort();
            String key = clientIP + ":" + clientPort;
            String clientPath = MyServer.BASE_PATH + clientPort;
            //MyServer.printFileList();
            //System.out.println(MyServer.PrintListFile());
            MyServer.processClientFiles(clientPath, key);
            while (true) {
                MyServer.processClientFiles(clientPath, key);
                String clientMessage = input.readUTF();
                //MyServer.printFileList();
                //System.out.println(MyServer.PrintListFile());
                if (clientMessage.equalsIgnoreCase("ALIVE")) {
                    System.out.println("Client with IP address " + socket.getInetAddress().getHostAddress() + " and Port: " + clientPort + " is still connected!\n");
                } 
                else if (clientMessage.contains("Finding file:")){
                    //String infoHash = clientMessage.split(":")[1];
                    boolean check = false;
                    String namefile = clientMessage.split(":")[1];
                    String infoHash;
                    if (MyServer.file_list2.containsKey(namefile)) {
                        //infoHash = MyServer.file_list2.get(namefile);
                        HashMap<String, HashMap<Integer, ArrayList<String>>> tmp = MyServer.file_list2.get(namefile);
                        infoHash = tmp.keySet().iterator().next();
                        if (MyServer.file_list.containsKey(infoHash)) {
                            
                            HashMap<Integer, ArrayList<String>> pieceMap = MyServer.file_list.get(infoHash);
                            output.writeUTF(Integer.toString(pieceMap.size()));

                            ///////////////////////////////////////////////////////////////////////////
                            /// RAREST STAGEDY ////
                            ///////////////////////////////////////////////////////////////////////////
                            // Convert map entries to a list for sorting
                            List<Map.Entry<Integer, ArrayList<String>>> entryList = new ArrayList<>(pieceMap.entrySet());
                            // Sort the entries by the size of the ArrayList in ascending order
                            entryList.sort(Comparator.comparingInt(entry -> entry.getValue().size()));
                            for (Map.Entry<Integer, ArrayList<String>> entry : entryList) {
                                if (entry.getValue().size() == 0) {
                                    check = true;
                                    output.writeUTF("File not found on the server.");
                                    break;
                                }
                                String str = Integer.toString(entry.getKey());
                                str = str + ";" + entry.getValue().get(0) + ";" + MyServer.BASE_PATH + entry.getValue().get(0).split(":")[1] + ";" + infoHash;
                                output.writeUTF(str);
                            }
                            ///////////////////////////////////////////////////////////////////////////
                            // for (Map.Entry<Integer, ArrayList<String>> entry : pieceMap.entrySet()){
                            //     if (entry.getValue().size()==0) {
                            //         check = true;
                            //         output.writeUTF("File not found on the server.");
                            //         break;
                            //     }
                            //     String str = Integer.toString(entry.getKey());
                            //     //str = str + ";" + entry.getValue().get(0) + ";" + MyServer.BASE_PATH + entry.getValue().get(0).split(":")[1]; //Integer.toString(clientPort);
                            //     str = str + ";" + entry.getValue().get(0) + ";" + MyServer.BASE_PATH + entry.getValue().get(0).split(":")[1] + ";" + infoHash;
                            //     output.writeUTF(str);
                            // }
                            if (check!=true) 
                            output.writeUTF("DONE");
                            //System.out.println("Sent piece-to-peer mapping for file with info_hash: " + infoHash);
                        } 
                    }
                    else {
                        output.writeUTF("File not found on the server.");
                    }
                }    
                else if (clientMessage.contains("PRINT")){
                     try {
                        output.writeUTF(MyServer.PrintListFile());
                     } catch (IOException e) {
                        System.out.println(e);
                     }
                }          
                else {
                    System.out.println("Received message from client " + clientPort + ": " + clientMessage);
                }
            }
        } catch (EOFException e) {
            //System.out.println("cho phuoc 1");
            System.out.println("Client " + clientPort + " disconnected!");
            cleanup();
            cleanup2();
        } catch (IOException e) {
            //System.out.println("cho phuoc 2");
            System.out.println("Client " + clientPort + " disconnected!");
            cleanup();
            cleanup2();
        }
    }

    private void cleanup() {
        try {
            // Remove client's pieces from tracker
            Iterator<Map.Entry<String, HashMap<Integer, ArrayList<String>>>> iterator = MyServer.file_list.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, HashMap<Integer, ArrayList<String>>> entry = iterator.next();
                HashMap<Integer, ArrayList<String>> pieceMap = entry.getValue();
                for (ArrayList<String> clients : pieceMap.values()) {
                    clients.remove(socket.getInetAddress().getHostAddress() + ":" + clientPort);
                }
            }
            
            if (!socket.isClosed()) {
                socket.close();
            }
            
            //System.out.println("Client " + clientPort + " handler cleaned up");
        } catch (IOException e) {
            System.out.println("Error during cleanup for client " + clientPort + ": " + e.getMessage());
        }
    }

    private void cleanup2() {
        try {
            // Iterator over file_list2
            Iterator<Map.Entry<String, HashMap<String, HashMap<Integer, ArrayList<String>>>>> bigIterator = MyServer.file_list2.entrySet().iterator();
            while (bigIterator.hasNext()) {
                Map.Entry<String, HashMap<String, HashMap<Integer, ArrayList<String>>>> bigEntry = bigIterator.next();
                HashMap<String, HashMap<Integer, ArrayList<String>>> fileMap = bigEntry.getValue();
    
                Iterator<Map.Entry<String, HashMap<Integer, ArrayList<String>>>> fileIterator = fileMap.entrySet().iterator();
                while (fileIterator.hasNext()) {
                    Map.Entry<String, HashMap<Integer, ArrayList<String>>> fileEntry = fileIterator.next();
                    HashMap<Integer, ArrayList<String>> pieceMap = fileEntry.getValue();
    
                    // Remove the client from all pieces
                    pieceMap.values().forEach(clients ->
                            clients.remove(socket.getInetAddress().getHostAddress() + ":" + clientPort));
    
                    // Check if all pieces are now empty
                    boolean allPiecesEmpty = pieceMap.values().stream().allMatch(List::isEmpty);
    
                    // If all pieces are empty, remove the file's key
                    if (allPiecesEmpty) {
                        fileIterator.remove(); // Remove this file entry from fileMap
                    }
                }
    
                // If the fileMap is now empty, remove the big key from file_list2
                if (fileMap.isEmpty()) {
                    bigIterator.remove();
                }
            }
    
            // Close the socket if not already closed
            if (!socket.isClosed()) {
                socket.close();
            }
            System.out.println("Client " + clientPort + " handler cleaned up");
        } catch (IOException e) {
            System.out.println("Error during cleanup for client " + clientPort + ": " + e.getMessage());
        }
    }
    
}