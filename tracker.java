import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MyServer {
    // Map<info_hash, Map<pieceIndex, List<clientKey>>>
    public static HashMap<String, HashMap<Integer, ArrayList<String>>> file_list = new HashMap<>();
    public static final String BASE_PATH = "D:" + File.separator + "Tamnd" + File.separator + "Tailieudh" + File.separator + "Computer network" + File.separator + "Project" + File.separator;

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

    public static String calculateInfoHash(MetaInfo.InfoDictionary info) {
        try {
            Map<String, Object> sortedInfoDict = new TreeMap<>();
            Map<String, Object> originalDict = info.toBEncodedDictionary();
            sortedInfoDict.putAll(originalDict);
            
            BEncoder encoder = new BEncoder();
            encoder.write(sortedInfoDict);
            String encodedInfo = encoder.toString();
            
            byte[] hash = MetaInfo.calculatePieceHash(encodedInfo.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(new String(hash, StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8.toString());
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
        if (files != null){    
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
                        }
        
                        int totalPieces = metaInfo.getInfo().getPieces().length / 20;

                        System.out.println("File name: " + file.getName());
                        System.out.println("Number of pieces: " + totalPieces);
                        
                        // Add client as having all pieces
                        HashMap<Integer, ArrayList<String>> pieceMap = file_list.get(infoHash);
                        for (int i = 0; i < totalPieces; i++) {
                            pieceMap.computeIfAbsent(i, k -> new ArrayList<>()).add(clientKey);
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

    public static Map<Integer, List<String>> getPieceAvailability(String infoHash) {
        return new HashMap<>(file_list.getOrDefault(infoHash, new HashMap<>()));
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
            String clientPath = MyServer.BASE_PATH + clientPort;
            
            MyServer.processClientFiles(clientPath, key);
            while (true) {
                String clientMessage = dis.readUTF();
                MyServer.printFileList();
                if (clientMessage.equalsIgnoreCase("ALIVE")) {
                    System.out.println("Client with IP address " + socket.getInetAddress().getHostAddress() + " and Port: " + clientPort + " is still connected!\n");
                } else {
                    System.out.println("Received message from client " + clientPort + ": " + clientMessage);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client " + clientPort + " disconnected!");
            cleanup();
        } catch (IOException e) {
            System.out.println("Client " + clientPort + " disconnected!");
            cleanup();
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
            
            System.out.println("Client " + clientPort + " handler cleaned up");
        } catch (IOException e) {
            System.out.println("Error during cleanup for client " + clientPort + ": " + e.getMessage());
        }
    }
}