package gitlet;

import java.io.Serializable;

public class MyUtils {
    /**
     * Returns the sha1 of a commit.
     * @param obj input obj
     * @return commit hash
     */
    static String commitHash(Serializable obj) {
        return Utils.sha1(Utils.serialize(obj));
    }
}
