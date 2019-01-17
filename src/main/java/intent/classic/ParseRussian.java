package intent.classic;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

class ParseRussian {

    private static LinkedHashMap<Character, Integer> alphabet;
    private HashMap<Integer, ArrayList<Character>> superTypes;
    private boolean isSuperTypes;

    ParseRussian(boolean useSuper){
        alphabet = new LinkedHashMap<>();
        superTypes = new HashMap<>();
        isSuperTypes = useSuper;
    }

    void readAlphabet(String filename) {
        String line;
        InputStream fis;

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(filename).getFile());
            fis = new FileInputStream(file);
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
            superTypes = this.splitToSuperTypes();
        }
    }

    HashMap<Integer, ArrayList<Character>> splitToSuperTypes() {
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

        HashMap<Integer, ArrayList<Character>> sTypes = new HashMap<>();
        sTypes.put(1, st1);
        sTypes.put(2, st2);
        sTypes.put(3, st3);
        sTypes.put(4, st4);
        sTypes.put(5, st5);

        return sTypes;
    }

    int getIntValue(char letter) {
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

    static char getCharValue(int num) {
        return getKeyByValue(alphabet, num);
    }

    static <T, E>
    T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}