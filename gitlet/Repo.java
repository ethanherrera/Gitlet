package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;


public class Repo {
    /** CWD. */
    static final File CWD = new File(System.getProperty("user.dir"));
    /** REPO_DIR. */
    static final File REPO_DIR = Utils.join
            (CWD, ".gitlet/");
    /** BRANCHES_DIR. */
    static final File BRANCHES_DIR = Utils.join
            (CWD, ".gitlet/branches/");
    /** COMMITS_DIR. */
    static final File COMMITS_DIR = Utils.join
            (CWD, ".gitlet/commits/");
    /** STAGE_FILE. */
    static final File STAGING_AREA_FILE = Utils.join
            (CWD, ".gitlet/staging-area/stage.txt");
    /** HEAD_BRANCH_FILE. */
    static final File HEAD_BRANCH_FILE = Utils.join
            (CWD, ".gitlet/pointers/HEAD-branch.txt");
    /** HEAD_COMMIT_FILE. */
    static final File HEAD_COMMIT_FILE = Utils.join
            (CWD, ".gitlet/pointers/HEAD-commit.txt");
    /** BLOBS_DIR. */
    static final File BLOBS_DIR = Utils.join
            (CWD, ".gitlet/blobs/");

    /** Repository stage. */
    private StagingArea stage;
    /** Head branch pointer. */
    private String headBranch;
    /** Head commit pointer. */
    private String headCommit;

    /** Constructor for the Repo class. */
    public Repo() {
        if (HEAD_BRANCH_FILE.exists()) {
            headBranch = Utils.readContentsAsString
                    (HEAD_BRANCH_FILE);
        } else {
            headBranch = "master";
        }

        if (HEAD_COMMIT_FILE.exists()) {
            headCommit = Utils.readContentsAsString
                    (HEAD_COMMIT_FILE);
        }

        if (STAGING_AREA_FILE.exists()) {
            stage = Utils.readObject
                    (STAGING_AREA_FILE, StagingArea.class);
        }
    }

    /**
     * Initializes the Gitlet repository if one doesn't already exist.
     */
    public void init() {
        if (REPO_DIR.exists()) {
            System.out.println("A Gitlet version-control system already "
                            + "exists in the current directory.");
            return;
        }
        String[] paths = new String[7];
        paths[0] = ".gitlet";
        paths[1] = ".gitlet/staging-area";
        paths[2] = ".gitlet/branches";
        paths[3] = ".gitlet/commits";
        paths[4] = ".gitlet/blobs";
        paths[5] = ".gitlet/global-log";
        paths[6] = ".gitlet/pointers";
        for (String path : paths) {
            assert path != null && !path.equals("");
            File newFile = Utils.join(CWD, path);
            newFile.mkdirs();
        }
        Commit initialCommit = new Commit(null,
                "initial commit", new HashMap<>(), getHEADBranch());
        File initialCommitFile = Utils.join
                (COMMITS_DIR, initialCommit.getHash() + ".txt");
        Utils.writeObject(initialCommitFile, initialCommit);

        File masterFile = Utils.join(BRANCHES_DIR, "master.txt");
        Utils.writeContents(masterFile, "master");

        updateHEAD("master", initialCommit.getHash());

        stage = new StagingArea();
        File stageFile = Utils.join(STAGING_AREA_FILE);
        Utils.writeObject(stageFile, stage);
    }

    /**
     * Adds a copy of the file as it currently exists to the staging area.
     * @param fileName input fileName
     */
    public void add(String fileName) {
        File addedFile = Utils.join(CWD, fileName);
        if (addedFile.exists()) {
            byte[] fileContents = Utils.readContents(addedFile);
            String fileHash = Utils.sha1(fileContents);
            if (stage.getRemoved().contains(fileName)) {
                stage.getRemoved().remove(fileName);
            }
            if (getHEADCommit().getBlobs().get(fileName) == null
                    || !Utils.sha1(getHEADCommit().getBlobs().get(fileName))
                    .equals(fileHash)) {
                File blobFile = Utils.join
                        (BLOBS_DIR, fileHash + ".txt");
                Utils.writeContents(blobFile, fileContents);
                stage.getAdded().put(fileName, fileContents);
            }
            updateStage();
        } else {
            System.out.println("File does not exist.");
        }
    }

    /**
     * Saves a snapshot of tracked files in
     * the current commit,
     * creates new commit.
     * @param message input message
     */
    public void commit(String message) {
        if (stage.getAdded().isEmpty() && stage.getRemoved().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (message == null || message.length() == 0) {
            System.out.println("Please enter a commit message");
            return;
        }
        Commit currentCommit = getHEADCommit();
        HashMap<String, byte[]> currentBlobs =
                Utils.cloneBlobHashmap(currentCommit.getBlobs());
        ArrayList<String> filesToAdd =
                new ArrayList<>(stage.getAdded().keySet());
        for (String fileName : filesToAdd) {
            currentBlobs.put(fileName, stage.getAdded().get(fileName));
        }
        for (String fileName : stage.getRemoved()) {
            currentBlobs.remove(fileName);
        }
        Commit newCommit = new Commit(currentCommit.getHash(),
                message, currentBlobs, getHEADBranch());
        Utils.writeObject(Utils.join
                (COMMITS_DIR, newCommit.getHash() + ".txt"), newCommit);
        updateHEAD(getHEADBranch(), newCommit.getHash());
        stage.clearAll();
        updateStage();

    }

    /**
     * Unstage the file if it is currently staged for addition.
     * @param fileName input fileName
     */
    public void rm(String fileName) {
        Commit currentCommit = getHEADCommit();
        boolean isStaged = stage.getAdded().containsKey(fileName);
        boolean isTracked = currentCommit.getBlobs().containsKey(fileName);
        if (!isStaged && !isTracked) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (isStaged) {
            stage.getAdded().remove(fileName);
        }
        if (isTracked) {
            Utils.restrictedDelete(fileName);
            stage.getRemoved().add(fileName);
        }
        updateStage();

    }

    /** Starting at the current head commit,
     * display information about each commit backward. */
    public void log() {
        Commit currentCommit = getHEADCommit();
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currentCommit.getHash());
            System.out.println("Date: " + currentCommit.getDateAndTime());
            System.out.println(currentCommit.getMessage());
            System.out.println();
            if (currentCommit.getParentHash() == null) {
                break;
            }
            currentCommit = Utils.readObject(Utils.join(COMMITS_DIR,
                    currentCommit.getParentHash() + ".txt"),
                    Commit.class);

        }
    }

    /** Like log, except displays information about all commits ever made. */
    public void globalLog() {
        ArrayList<String> filesInCommitDir =
                new ArrayList<>(Utils.plainFilenamesIn(COMMITS_DIR));
        for (String fileName : filesInCommitDir) {
            Commit currentCommit = Utils.readObject(Utils.join
                    (COMMITS_DIR, fileName),
                    Commit.class);
            System.out.println("===");
            System.out.println("commit " + currentCommit.getHash());
            System.out.println("Date: " + currentCommit.getDateAndTime());
            System.out.println(currentCommit.getMessage());
            System.out.println();
        }
    }

    /**
     * Prints out the ids of all commits
     * that have the given commit message, one per line.
     * @param commitMessage input commitMessage
     */
    public void find(String commitMessage) {
        ArrayList<String> filesInCommitDir =
                new ArrayList<>(Utils.plainFilenamesIn(COMMITS_DIR));
        boolean matchingMessage = false;
        for (String fileName : filesInCommitDir) {
            Commit commitPointer = Utils.readObject
                    (Utils.join(COMMITS_DIR, fileName),
                            Commit.class);
            if (commitPointer.getMessage().equals(commitMessage)) {
                System.out.println(commitPointer.getHash());
                matchingMessage = true;
            }
        }
        if (!matchingMessage) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist
     * and what files have been staged for
     * addition or removal. */
    public void status() {
        ArrayList<String> filesInBranchDir =
                new ArrayList<>(Utils.plainFilenamesIn(BRANCHES_DIR));

        System.out.println("=== Branches ===");
        for (String branchName : filesInBranchDir) {
            branchName = branchName.substring(0, branchName.length() - 4);
            if (branchName.equals(headBranch)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        ArrayList<String> stagedFiles =
                new ArrayList<>(stage.getAdded().keySet());
        Collections.sort(stagedFiles);
        for (String fileName : stagedFiles) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        Collections.sort(stage.getRemoved());
        for (String fileName : stage.getRemoved()) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    /**
     * Takes the file from the front of the current branch,
     * and puts it in the working directory.
     * @param fileName input fileName
     */
    public void checkoutFileName(String fileName) {
        Commit currCommit = getHEADCommit();
        HashMap<String, byte[]> headBlobs = currCommit.getBlobs();

        if (!headBlobs.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
        } else {
            File fileBlob = Utils.join(BLOBS_DIR,
                    Utils.sha1(currCommit.getBlobs().get(fileName))
                            + ".txt");
            byte[] contents = Utils.readContents(fileBlob);
            Utils.writeContents(Utils.join(CWD, fileName), contents);
        }
    }

    /**
     * Takes the file from the commit with commitID,
     * and puts it in the working directory.
     * @param commitID input commitID
     * @param fileName input fileName
     */
    public void checkoutCommitID(String commitID, String fileName) {
        Commit fetchedCommit = getCommitWithID(commitID);
        if (!fetchedCommit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        if (getCommitWithID(commitID).getBlobs().containsKey(fileName)
                && !getHEADCommit().getBlobs().containsKey(fileName)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
        File fileBlob = Utils.join(BLOBS_DIR,
                Utils.sha1(fetchedCommit.getBlobs().get(fileName))
                        + ".txt");
        byte[] contents = Utils.readContents(fileBlob);
        Utils.writeContents(Utils.join(CWD, fileName), contents);
    }

    /**
     * Checkout helper function for reset.
     * @param commit input commit
     * @param fileName input fileName
     */
    public void checkoutCommitReset(Commit commit, String fileName) {
        if (commit.getBlobs().containsKey(fileName)
                && !getHEADCommit().getBlobs().containsKey(fileName)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
        if (commit.getBlobs().get(fileName) != null) {
            File fileBlob = Utils.join(BLOBS_DIR,
                    Utils.sha1(commit.getBlobs().get(fileName))
                            + ".txt");
            if (fileBlob.exists()) {
                byte[] contents = Utils.readContents(fileBlob);
                Utils.writeContents(Utils.join(CWD, fileName), contents);
            }
        }
    }

    /**
     * Takes all files in the commit at the head
     * of the given branch,
     * and puts them in the working directory.
     * @param branchName input branchName
     */
    public void checkoutBranchName(String branchName) {
        if (getHEADBranch().equals(branchName)) {
            System.out.println("No need to checkout the current branch");
            return;
        }
        ArrayList<String> filesInCWD =
                new ArrayList<>(Utils.plainFilenamesIn(CWD));
        ArrayList<String> filesInBranches =
                new ArrayList<>(Utils.plainFilenamesIn(BRANCHES_DIR));
        if (!filesInBranches.contains(branchName + ".txt")) {
            System.out.println("No such branch exists.");
            return;
        }
        Commit checkoutCommit = getCommitWithID(Utils.readContentsAsString
                (Utils.join(BRANCHES_DIR, branchName
                        + ".txt")));
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (checkoutCommit.getBlobs().containsKey(fileName)
                    && !getHEADCommit().getBlobs().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        for (String fileName : filesInCWD) {
            File file = Utils.join(CWD, fileName);
            if (checkoutCommit.getBlobs().containsKey(fileName)) {
                Utils.writeContents(file,
                        checkoutCommit.getBlobs().get(fileName));
            } else {
                Utils.restrictedDelete(file);
            }
        }
        for (String fileName : checkoutCommit.getBlobs().keySet()) {
            if (!filesInCWD.contains(fileName)) {
                File fileBlob = Utils.join(BLOBS_DIR,
                        Utils.sha1(checkoutCommit.getBlobs().get(fileName))
                                + ".txt");
                if (fileBlob.exists()) {
                    byte[] contents = Utils.readContents(fileBlob);
                    Utils.writeContents(Utils.join(CWD, fileName), contents);
                }
            }
        }
        updateHEAD(branchName, checkoutCommit.getHash());
    }

    /**
     * Creates a new branch with the given name,
     * and points it at the current head node.
     * @param branchName input branchName
     */
    public void branch(String branchName) {
        File newBranch = Utils.join(BRANCHES_DIR, branchName + ".txt");
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            Utils.writeContents(newBranch, getHEADCommit().getHash());
        }
    }

    /**
     * Deletes the branch with the given name.
     * @param branchName input branchName
     */
    public void rmBranch(String branchName) {
        if (branchName.equals(getHEADBranch())) {
            System.out.println("Cannot remove the current branch.");
        }
        File branchFile = Utils.join(BRANCHES_DIR, branchName + ".txt");
        if (!branchFile.delete()) {
            System.out.println("A branch with that name does not exist.");
        }
    }

    /**
     * Checks out all the files tracked by the given commit.
     * @param commitID input commitID
     */
    public void reset(String commitID) {
        Commit targetCommit = getCommitWithID(commitID);
        ArrayList<String> filesInCWD =
                new ArrayList<>(Utils.plainFilenamesIn(CWD));
        for (String fileName : filesInCWD) {
            checkoutCommitReset(targetCommit, fileName);
        }
        stage.clearAll();
        updateStage();
        updateHEAD(getBranchFromCommitID(commitID), targetCommit.getHash());
    }

    /**
     * Gets the split point of two branches.
     * @param headPointer input commit
     * @param mergeCommit input mergeCommit
     * @return splitPoint commit
     */
    private Commit getSplitPoint(Commit headPointer, Commit mergeCommit) {
        ArrayList<String> currentBranchCommits = new ArrayList<>();
        Commit headBranchPointer = headPointer;
        while (true) {
            currentBranchCommits.add(headBranchPointer.getHash());
            if (headBranchPointer.getParentHash() == null) {
                break;
            }
            headBranchPointer =
                    Utils.readObject(Utils.join(COMMITS_DIR,
                            headBranchPointer.getParentHash()
                                    + ".txt"), Commit.class);
        }
        Commit mergeBranchPointer = mergeCommit;
        while (!currentBranchCommits.contains(mergeBranchPointer.getHash())) {
            if (mergeBranchPointer.getParentHash() == null) {
                break;
            }
            mergeBranchPointer =
                    Utils.readObject(Utils.join(COMMITS_DIR,
                            mergeBranchPointer.getParentHash()
                                    + ".txt"), Commit.class);
        }
        return mergeBranchPointer;
    }

    /**
     * Determines whether of not merge will fail.
     * @param branchName input branchName
     * @return a boolean determining if the merge will fail
     */
    private boolean mergeFailureCases(String branchName) {
        if (stage.getAdded().size() > 0 || stage.getRemoved().size() > 0) {
            System.out.println("You have uncommitted changes.");
            return false;
        } else if (!Utils.join(BRANCHES_DIR, branchName + ".txt").exists()) {
            System.out.println("A branch with that name does not exist.");
            return false;
        } else if (branchName.equals(getHEADBranch())) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!getHEADCommit().getBlobs().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return false;
            }
        }
        return true;
    }

    /**
     * Merges files from the given branch into the current branch.
     * @param branchName target branchName
     */
    public void merge(String branchName) {
        if (!mergeFailureCases(branchName)) {
            return;
        }
        Commit currentCommit = getHEADCommit();
        Commit mergeCommit = getCommitWithID(Utils.readContentsAsString(
                Utils.join(BRANCHES_DIR, branchName
                        + ".txt")));
        Commit splitPoint = getSplitPoint(currentCommit, mergeCommit);
        if (splitPoint.getHash().equals(mergeCommit.getHash())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        }
        if (splitPoint.getHash().equals((currentCommit.getHash()))) {
            checkoutBranchName(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        HashMap<String, byte[]> headBlobs = currentCommit.getBlobs();
        HashMap<String, byte[]> mergeBlobs = mergeCommit.getBlobs();
        HashMap<String, byte[]> splitBlobs = splitPoint.getBlobs();
        HashSet<String> allBlobs = blobCombiner3
                (headBlobs, mergeBlobs, splitBlobs);
        HashMap<String, byte[]> newBlobs = new HashMap<>();
        for (String fileName : allBlobs) {
            byte[] headVersion = headBlobs.get(fileName);
            byte[] mergeVersion = mergeBlobs.get(fileName);
            byte[] splitVersion = splitBlobs.get(fileName);
            File headFile = new File("");
            if (headVersion != null) {
                headFile = Utils.join(BLOBS_DIR,
                        Utils.sha1(headVersion) + ".txt");
            }
            File mergeFile = new File("");
            if (mergeVersion != null) {
                mergeFile = Utils.join(BLOBS_DIR,
                        Utils.sha1(mergeVersion) + ".txt");
            }
            File currentFile = Utils.join(CWD, fileName);
            if ((Arrays.equals(mergeVersion, splitVersion)
                    && Arrays.equals(headVersion, mergeVersion))
                    || (Arrays.equals(mergeVersion, headVersion)
                    && !Arrays.equals(mergeVersion, splitVersion))) {
                newBlobs.put(fileName, headVersion);
            } else if (!Arrays.equals(mergeVersion, splitVersion)
                    && Arrays.equals(splitVersion, headVersion)) {
                newBlobs.put(fileName, mergeVersion);
                overwriteOrRemove(fileName, mergeFile,
                        currentFile, mergeVersion);
            } else if (!Arrays.equals(mergeVersion, headVersion)
                    && !Arrays.equals(mergeVersion, splitVersion)
                    && !Arrays.equals(headVersion, splitVersion)) {
                resolveConflict(fileName, currentFile, headFile,
                        mergeFile, headVersion, mergeVersion);
            }
        }
        commitMergeCommit(currentCommit, mergeCommit, branchName, newBlobs);
    }

    /**
     * Merge helper function to overwrite or remove.
     * @param fileName input file name
     * @param mergeFile input merge file
     * @param currentFile input current file
     * @param blob input blob
     */
    private void overwriteOrRemove(String fileName, File mergeFile,
                                   File currentFile, byte[] blob) {
        if (mergeFile.exists() && blob != null) {
            byte[] contents = Utils.readContents(mergeFile);
            Utils.writeContents(currentFile, contents);
            stage.getAdded().put(fileName, contents);
        } else {
            Utils.restrictedDelete(currentFile);
            stage.getRemoved().add(fileName);
        }
    }

    private void resolveConflict(String fileName, File currentFile,
                                 File headFile, File mergeFile,
                                 byte[] headBlob, byte[] mergeBlob) {
        String mergeContent = "";
        String headContent = "";
        if (mergeFile.exists() && mergeBlob != null) {
            mergeContent = new String(Utils.readContents(mergeFile));
        }
        if (headFile.exists() && headBlob != null) {
            headContent = new String(Utils.readContents(headFile));
        }
        String contentToWrite = "<<<<<<< HEAD\n" + headContent
                + "=======\n" + mergeContent + ">>>>>>>\n";
        Utils.writeContents(currentFile, contentToWrite);
        stage.getAdded().put(fileName, contentToWrite.getBytes());
        System.out.println("Encountered merge conflict.");
    }

    private void commitMergeCommit(Commit currentCommit, Commit mergeCommit,
                                   String branchName, HashMap<String,
                                    byte[]> newBlobs) {
        MergeCommit mergedCommit =
                new MergeCommit(currentCommit.getHash(),
                mergeCommit.getHash(),
                "Merged " + branchName + " into "
                        + getHEADBranch() + ".", newBlobs,
                        getHEADBranch());
        Utils.writeObject(Utils.join
                (COMMITS_DIR, mergedCommit.getHash()
                        + ".txt"), mergedCommit);
        updateHEAD(getHEADBranch(), mergedCommit.getHash());
        stage.clearAll();
        updateStage();
    }

    /**
     * Updates Head.
     * @param branch input branch
     * @param commit input commit
     */
    private void updateHEAD(String branch, String commit) {
        headCommit = commit;
        headBranch = branch;
        Utils.writeContents(Utils.join
                (CWD, ".gitlet/pointers/HEAD-branch.txt"), headBranch);
        Utils.writeContents(Utils.join
                (CWD, ".gitlet/pointers/HEAD-commit.txt"), headCommit);
        Utils.writeContents(Utils.join
                (BRANCHES_DIR, headBranch + ".txt"), headCommit);
    }

    /** Updates Stage field. */
    private void updateStage() {
        Utils.writeObject(STAGING_AREA_FILE, stage);
    }

    /**
     * Updates head branch.
     * @return string branch
     */
    private String getHEADBranch() {
        return headBranch;
    }

    /**
     * Updates head commit.
     * @return head commit
     */
    private Commit getHEADCommit() {
        return Utils.readObject(Utils.join
                (COMMITS_DIR, headCommit + ".txt"), Commit.class);
    }

    /**
     * Fetches commit with the provided ID.
     * @param commitID input commit ID
     * @return commit
     */
    private Commit getCommitWithID(String commitID) {
        String fetchedCommitID = "";
        ArrayList<String> filesInCommitDir =
                new ArrayList<>(Utils.plainFilenamesIn(COMMITS_DIR));
        for (String targetFileName : filesInCommitDir) {
            if (targetFileName.startsWith(commitID)) {
                fetchedCommitID = targetFileName.substring
                        (0, targetFileName.length() - 4);
                break;
            }
        }
        if (fetchedCommitID.equals("")) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
            return null;
        }
        return Utils.readObject(Utils.join
                (COMMITS_DIR, fetchedCommitID + ".txt"),
                Commit.class);
    }

    /**
     * Fetches branch from corresponding commmitID.
     * @param commitID input commitID
     * @return string branch
     */
    private String getBranchFromCommitID(String commitID) {
        ArrayList<String> filesInBranchesDir =
                new ArrayList<>(Utils.plainFilenamesIn(BRANCHES_DIR));
        Commit commit = getCommitWithID(commitID);
        for (String fileName : filesInBranchesDir) {
            String substring = fileName.substring
                    (0, fileName.length() - 4);
            if (substring.equals(commit.getBranch())) {
                return substring;
            }
        }
        return null;
    }

    /**
     * Blob combiner to set.
     * @param blob1 input 1
     * @param blob2 input 2
     * @param blob3 input 3
     * @return set of blobs
     */
    private HashSet<String> blobCombiner3(HashMap<String, byte[]> blob1,
                                          HashMap<String, byte[]> blob2,
                                          HashMap<String, byte[]> blob3) {
        HashSet<String> set = new HashSet<>();
        set.addAll(blob1.keySet());
        set.addAll(blob2.keySet());
        set.addAll(blob3.keySet());
        return set;
    }
}
