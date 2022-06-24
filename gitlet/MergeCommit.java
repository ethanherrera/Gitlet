package gitlet;

import java.util.HashMap;

public class MergeCommit extends Commit {
    /** The step parent of the commit. */
    protected String stepParent;
    MergeCommit(String parHash, String stepParHash, String inputMessage,
                HashMap<String, byte[]> referencedBlobs,
                String belongingBranch) {
        super(parHash, inputMessage, referencedBlobs, belongingBranch);
        stepParent = stepParHash;
    }

    /**
     * Returns step parent.
     * @return step parent
     */
    public String getStepParent() {
        return stepParent;
    }

    /**
     *  Sets step parent.
     * @param inputStepParent the set step parent
     */
    public void setStepParent(String inputStepParent) {
        this.stepParent = inputStepParent;
    }
}
