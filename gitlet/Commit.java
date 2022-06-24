package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Commit implements Serializable {
    /** sha1. */
    protected final String hash;
    /** parent sha1. */
    protected final String parentHash;
    /** commit date and time. */
    protected final String dateAndTime;
    /** commit message. */
    protected final String message;
    /** the branch the commit belongs to. */
    protected final String branch;
    /** HashMap mapped String fileName to byte[] contents. */
    protected final HashMap<String, byte[]> blobs;

    Commit(String parHash, String inputMessage,
           HashMap<String, byte[]> referencedBlobs, String belongingBranch) {
        if (parHash == null) {
            parentHash = null;
            dateAndTime = "Wed Dec 31 16:00:00 1969 -0800";
        } else {
            parentHash = parHash;
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy");
            LocalDateTime ldt = LocalDateTime.now();
            dateAndTime = formatter.format(ldt) + " -0800";
        }
        message = inputMessage;
        blobs = referencedBlobs;
        branch = belongingBranch;
        hash = MyUtils.commitHash(this);
    }

    /**
     * get hash.
     * @return hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * get parent hash.
     * @return parent hash
     */
    public String getParentHash() {
        return parentHash;
    }

    /**
     * get dateAndTime.
     * @return dateAndTime
     */
    public String getDateAndTime() {
        return dateAndTime;
    }

    /**
     * get message.
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * get blobs.
     * @return blobs map
     */
    public HashMap<String, byte[]> getBlobs() {
        return blobs;
    }

    /**
     * get branch.
     * @return branch
     */
    public String getBranch() {
        return branch;
    }

    /** get log. */
    public void log() {
        Utils.writeContents(new File(".gitlet/global-log/log.txt"), toString());
    }
}
