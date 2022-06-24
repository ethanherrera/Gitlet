package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class StagingArea implements Serializable {
    /** HashMap of added files. */
    private final HashMap<String, byte[]> added;
    /** ArrayList<> of removed files. */
    private final ArrayList<String> removed;

    StagingArea() {
        added = new HashMap<>();
        removed = new ArrayList<>();
    }

    /**
     * gets added.
     * @return added
     */
    public HashMap<String, byte[]> getAdded() {
        return added;
    }

    /**
     * gets removed.
     * @return removed
     */
    public ArrayList<String> getRemoved() {
        return removed;
    }

    /** clears all fields in staging already. */
    public void clearAll() {
        added.clear();
        removed.clear();
    }
}
