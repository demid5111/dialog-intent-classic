package intent.classic;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

class ParseRussian {

    static boolean isSuperTypes = true;
    static LinkedHashMap<Character, Integer> alphabet = new LinkedHashMap<Character, Integer>();
    static HashMap<Integer, ArrayList<Character>> superTypes = new HashMap();

    public static void readAlphabet(String filename) {
        String line;
        InputStream fis;
        try {
            fis = new FileInputStream(filename);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(isr);
            int i = 1;
            while ((line = br.readLine()) != null) {
                char rusChar = line.charAt(0);
                alphabet.put(rusChar, i);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isSuperTypes) {
            splitToSuperTypes();
        }
    }

    private static void splitToSuperTypes() {
        ArrayList<Character> st1 = new ArrayList<Character>();
        st1.add('а');
        st1.add('б');
        st1.add('в');
        st1.add('г');
        st1.add('д');

        ArrayList<Character> st2 = new ArrayList<Character>();
        st2.add('е');
        st2.add('ж');
        st2.add('з');
        st2.add('и');
        st2.add('к');

        ArrayList<Character> st3 = new ArrayList<Character>();
        st3.add('л');
        st3.add('м');
        st3.add('н');
        st3.add('о');
        st3.add('п');

        ArrayList<Character> st4 = new ArrayList<Character>();
        st4.add('р');
        st4.add('с');
        st4.add('т');
        st4.add('у');
        st4.add('ф');

        ArrayList<Character> st5 = new ArrayList<Character>();
        st5.add('х');
        st5.add('ц');
        st5.add('ч');
        st5.add('ш');
        st5.add('щ');

        superTypes.put(1, st1);
        superTypes.put(2, st2);
        superTypes.put(3, st3);
        superTypes.put(4, st4);
        superTypes.put(5, st5);
    }

    public static int getIntValue(char letter) {
        if (!isSuperTypes) {
            return alphabet.get(letter);
        } else {
            for (Map.Entry<Integer, ArrayList<Character>> entry : superTypes.entrySet()) {
                if (entry.getValue().contains(letter)) {
                    return entry.getKey();
                }

            }
        }
        return -1;
    }

    public static char getCharValue(int num) {
        return getKeyByValue(alphabet, num);
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}