package raft.common;

import net.whitbeck.rdbparser.Entry;
import net.whitbeck.rdbparser.EntryType;
import net.whitbeck.rdbparser.KeyValuePair;
import net.whitbeck.rdbparser.RdbParser;

import java.io.File;
import java.io.IOException;

public class RDBParser {

    /**
     * Parse given file and get value for given key
     * Prerequisite: A key only has one value
     *
     * @param file
     * @param key
     * @return
     */
    public static String getVal(File file, String key) {
        try (RdbParser rdbParser = new RdbParser(file)) {
            Entry e;
            while ((e = rdbParser.readNext()) != null) {
                if (e.getType() != EntryType.KEY_VALUE_PAIR) continue;
                KeyValuePair kv = (KeyValuePair) e;
                String kvKey = new String(kv.getKey(), "ASCII");
                if (kvKey.equals(key)) {
                    return new String(kv.getValues().get(0), "ASCII");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
