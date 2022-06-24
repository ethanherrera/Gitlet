package gitlet;

import java.util.Arrays;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Ethan Herrera
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String[] ops = Arrays.copyOfRange(args, 1, args.length);
        Repo repo = new Repo();
        switch (args[0]) {
        case "init":
            initHelper(repo, ops);
            break;
        case "add":
            addHelper(repo, ops);
            break;
        case "commit":
            commitHelper(repo, ops);
            break;
        case "rm":
            removeHelper(repo, ops);
            break;
        case "log":
            logHelper(repo, ops);
            break;
        case "global-log":
            globalLogHelper(repo, ops);
            break;
        case "find":
            findHelper(repo, ops);
            break;
        case "status":
            statusHelper(repo, ops);
            break;
        case "checkout":
            checkoutHelper(repo, ops);
            break;
        case "branch":
            if (isInit(repo) && numOps(1, ops.length)) {
                repo.branch(ops[0]);
            }
            break;
        case "rm-branch":
            if (isInit(repo) && numOps(1, ops.length)) {
                repo.rmBranch(ops[0]);
            }
            break;
        case "reset":
            if (isInit(repo) && numOps(1, ops.length)) {
                repo.reset(ops[0]);
            }
            break;
        case "merge":
            if (isInit(repo) && numOps(1, ops.length)) {
                repo.merge(ops[0]);
            }
            break;
        default:
            System.out.println("No command with that name exists.");
        }
        System.exit(0);
    }

    /**
     * Checks if gitlet repo is initialized.
     * @param repo input repo
     * @return boolean continue with action
     */
    private static boolean isInit(Repo repo) {
        if (!repo.REPO_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return false;
        }
        return true;
    }

    /**
     * Confirms the number of ops is correct.
     * @param correct input correct
     * @param input input input int
     * @return if correct number
     */
    private static boolean numOps(int correct, int input) {
        if (correct == input) {
            return true;
        } else {
            System.out.println("Incorrect operands.");
            return false;
        }
    }

    /**
     * Confirms the number of ops is correct without printing.
     * @param correct input correct
     * @param input input input int
     * @return if correct number
     */
    private static boolean numOpsPl(int correct, int input) {
        return correct == input;
    }

    private static void checkoutHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOpsPl
                (2, ops.length) && ops[0].equals("--")) {
            repo.checkoutFileName(ops[1]);
        } else if (isInit(repo) && numOpsPl(3, ops.length)
                && ops[1].equals("--")) {
            repo.checkoutCommitID(ops[0], ops[2]);
        } else if (isInit(repo) && numOps(1, ops.length)) {
            repo.checkoutBranchName(ops[0]);
        }
    }

    private static void initHelper(Repo repo, String[] ops) {
        if (numOps(0, ops.length)) {
            repo.init();
        }
    }

    private static void addHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOps(1, ops.length)) {
            repo.add(ops[0]);
        }
    }

    private static void commitHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOps(1, ops.length)) {
            repo.commit(ops[0]);
        }
    }

    private static void removeHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOps(1, ops.length)) {
            repo.rm(ops[0]);
        }
    }

    private static void logHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOps(0, ops.length)) {
            repo.log();
        }
    }

    private static void globalLogHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOps(0, ops.length)) {
            repo.globalLog();
        }
    }

    private static void findHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOps(1, ops.length)) {
            repo.find(ops[0]);
        }
    }

    private static void statusHelper(Repo repo, String[] ops) {
        if (isInit(repo) && numOps(0, ops.length)) {
            repo.status();
        }
    }
}
