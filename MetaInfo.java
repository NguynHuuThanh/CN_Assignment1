import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MetaInfo {
    private String announce;
    private InfoDictionary info;

    public String getAnnounce() {
        return announce;
    }
    
    public InfoDictionary getInfo() {
        return info;
    }

    public static class InfoDictionary {
        private int pieceLength;
        private byte[] pieces;
        private String name;
        
        private Long length; // single file
        private List<FileInfo> files; // multiple file

        public String getName() {
            return name;
        }
        
        public int getPieceLength() {
            return pieceLength;
        }
        
        public byte[] getPieces() {
            return pieces;
        }
        
        public Long getLength() {
            return length;
        }
        
        public List<FileInfo> getFiles() {
            return files;
        }
        
        public boolean isMultiFile() {
            return files != null;
        }
        
        public static class FileInfo {
            private long length;
            private List<String> path;

            public long getLength() {
                return length;
            }
            
            public List<String> getPath() {
                return path;
            }
            
            public FileInfo(long length, List<String> path) {
                this.length = length;
                this.path = path;
            }
            public Map<String, Object> toBEncodedDictionary() {
                Map<String, Object> dict = new LinkedHashMap<>();
                dict.put("length", length);
                dict.put("path", path);
                return dict;
            }
        }
        
        public Map<String, Object> toBEncodedDictionary() {
            Map<String, Object> dict = new LinkedHashMap<>();
            dict.put("piece length", pieceLength);
            dict.put("pieces", pieces);
            dict.put("name", name);
            
            if (files != null) {
                List<Map<String, Object>> filesList = new ArrayList<>();
                for (FileInfo file : files) {
                    filesList.add(file.toBEncodedDictionary());
                }
                dict.put("files", filesList);
            } else {
                dict.put("length", length);
            }
            return dict;
        }
    }
    
    public MetaInfo() {
        this.info = new InfoDictionary();
    }
    
    public void setAnnounce(String announce) {
        this.announce = announce;
    }
    
    public void setInfo(String name, int pieceLength) {
        info.name = name;
        info.pieceLength = pieceLength;
    }
    
    public void setSingleFile(long length) {
        info.length = length;
        info.files = null;
    }
    
    public void setMultipleFiles(List<InfoDictionary.FileInfo> files) {
        info.files = files;
        info.length = null;
    }
    
    public void setPieces(List<byte[]> pieceHashes) {
        byte[] pieces = new byte[pieceHashes.size() * 20];
        for (int i = 0; i < pieceHashes.size(); i++) {
            System.arraycopy(pieceHashes.get(i), 0, pieces, i * 20, 20);
        }
        info.pieces = pieces;
    }

    public static void setFilePieces(MetaInfo metaInfo, File file) throws IOException, NoSuchAlgorithmException {
        int pieceLength = metaInfo.getInfo().getPieceLength();
        List<byte[]> pieceHashes = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[pieceLength];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                if (bytesRead < pieceLength) {
                    byte[] lastPiece = new byte[bytesRead];
                    System.arraycopy(buffer, 0, lastPiece, 0, bytesRead);
                    pieceHashes.add(MetaInfo.calculatePieceHash(lastPiece));
                } else {
                    pieceHashes.add(MetaInfo.calculatePieceHash(buffer));
                }
            }
        }
        
        metaInfo.setPieces(pieceHashes);
    }
    
    public String encode() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("info", info.toBEncodedDictionary());
        dict.put("announce", announce);
        
        BEncoder encoder = new BEncoder();
        encoder.write(dict);
        return encoder.toString();
    }
    
    public void saveToFile(String filePath) throws IOException {
        if (!filePath.toLowerCase().endsWith(".torrent")) {
            filePath += ".torrent";
        }
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            String encodedData = encode();
            fos.write(encodedData.getBytes("UTF-8"));
        }
    }

    public static MetaInfo loadFromFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] data = new byte[fis.available()];
            fis.read(data);
            String bencodedData = new String(data, "UTF-8");
            return decode(bencodedData);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static MetaInfo decode(String bencodedData) {
        BEncoder decoder = new BEncoder(bencodedData);
        Map<String, Object> dict = (Map<String, Object>) decoder.read();
        
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.announce = (String) dict.get("announce");
        
        Map<String, Object> infoDict = (Map<String, Object>) dict.get("info");
        InfoDictionary info = new InfoDictionary();
        
        info.pieceLength = ((Long) infoDict.get("piece length")).intValue();
        
        Object piecesObj = infoDict.get("pieces");
        if (piecesObj instanceof ArrayList) {
            ArrayList<Object> piecesList = (ArrayList<Object>) piecesObj;
            info.pieces = new byte[piecesList.size()];
            for (int i = 0; i < piecesList.size(); i++) {
                if (piecesList.get(i) instanceof Number) {
                    info.pieces[i] = ((Number) piecesList.get(i)).byteValue();
                }
            }
        }
        
        info.name = (String) infoDict.get("name");
        
        if (infoDict.containsKey("files")) {
            List<Map<String, Object>> filesList = (List<Map<String, Object>>) infoDict.get("files");
            List<InfoDictionary.FileInfo> files = new ArrayList<>();
            
            for (Map<String, Object> fileDict : filesList) {
                long length = (Long) fileDict.get("length");
                List<String> path = (List<String>) fileDict.get("path");
                files.add(new InfoDictionary.FileInfo(length, path));
            }
            
            info.files = files;
        } else {
            info.length = (Long) infoDict.get("length");
        }
        
        metaInfo.info = info;
        return metaInfo;
    }
    
    public static byte[] calculatePieceHash(byte[] pieceData) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return digest.digest(pieceData);
    }
}