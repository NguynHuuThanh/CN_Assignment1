import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class BEncoder {
    private String in;
    private StringBuilder sb;
    private int id;
    
    public BEncoder() {
        sb = new StringBuilder();
    }
    
    public BEncoder(String in) {
        this();
        this.in = in;
    }
    
    public void setInput(String in) {
        this.in = in;
        this.id = 0;
    }
    
    public void resetOutput() {
        sb.setLength(0);
    }
    
    public void write(Object o) {
        if (o == null) return;
        
        if (o instanceof Map) {
            sb.append('d');
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                write(e.getKey());
                write(e.getValue());
            }
            sb.append('e');
        } else if (o instanceof Iterable) {
            sb.append('l');
            for (Object item : (Iterable<?>) o) {
                write(item);
            }
            sb.append('e');
        } else if (o.getClass().isArray()) {
            sb.append('l');
            if (o instanceof Object[]) {
                for (Object item : (Object[]) o) {
                    write(item);
                }
            } else {
                int len = Array.getLength(o);
                for (int i = 0; i < len; i++) {
                    write(Array.get(o, i));
                }
            }
            sb.append('e');
        } else if (o instanceof String) {
            String str = (String) o;
            sb.append(str.length()).append(':').append(str);
        } else if (o instanceof Number) {
            sb.append('i').append(o).append('e');
        } else if (o instanceof Boolean) {
            sb.append('i').append((Boolean) o ? '1' : '0').append('e');
        }
    }
    
    public void writeAll(Object... objects) {
        for (Object o : objects) {
            write(o);
        }
    }
    
    public void writeAll(Iterable<Object> objects) {
        for (Object o : objects) {
            write(o);
        }
    }
    
    public ArrayList<Object> readAll() {
        ArrayList<Object> result = new ArrayList<>();
        while (id < in.length()) {
            Object o = read();
            if (o == null) break;
            result.add(o);
        }
        return result;
    }
    
    public Object read() {
        if (id >= in.length()) return null;
        
        char type = in.charAt(id++);
        
        if (type == 'i') {
            long value = 0;
            boolean isNegative = false;
            
            if (in.charAt(id) == '-') {
                isNegative = true;
                id++;
            }
            
            while (id < in.length()) {
                char c = in.charAt(id);
                if (c == 'e') {
                    id++;
                    return isNegative ? -value : value;
                }
                if (c < '0' || c > '9') break;
                value = value * 10 + (c - '0');
                id++;
            }
        } else if (type == 'l') {
            ArrayList<Object> list = new ArrayList<>();
            while (id < in.length() && in.charAt(id) != 'e') {
                Object item = read();
                if (item == null) break;
                list.add(item);
            }
            if (id < in.length() && in.charAt(id) == 'e') id++;
            return list;
        } else if (type == 'd') {
            LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
            while (id < in.length() && in.charAt(id) != 'e') {
                Object key = read();
                if (key == null) break;
                Object value = read();
                if (value == null) break;
                map.put(key, value);
            }
            if (id < in.length() && in.charAt(id) == 'e') id++;
            return map;
        } else if (type >= '0' && type <= '9') {
            id--; // Move back to include first digit
            int length = 0;
            
            // Parse the length digits
            while (id < in.length()) {
                char c = in.charAt(id);
                if (c == ':') {
                    id++;
                    if (id + length <= in.length()) {
                        String result = in.substring(id, id + length);
                        id += length;
                        return result;
                    }
                    return null;
                }
                if (c < '0' || c > '9') return null;
                length = length * 10 + (c - '0');
                id++;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return sb.toString();
    }
}