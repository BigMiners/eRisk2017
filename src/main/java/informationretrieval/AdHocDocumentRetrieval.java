package informationretrieval;

import common.Configs;
import common.User;
import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.ListResult;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.Parser;

import java.io.IOException;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;


/**
 * Scenarios available :
 * mlt1 : for each user of the test set, retrieve the 20 first matching documents compared to the user totalProduction. Executed against eriskMoreLikeThis core.
 * mlt2 : for each user of the test set, retrieve the 20 first matching documents compared to the user totalProduction. Executed against eriskRun2 core.
 */

public class AdHocDocumentRetrieval {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<User> Users;
    private String chunkId;
    private String baseUrl;
    private HashMap<String, String> results = new HashMap<>();
    private String test_corpora_directory;
    private String task;
    private HashSet<User> _usersHashSet = new HashSet<>();
    private String RESULTS_FOLDER;

    public AdHocDocumentRetrieval() {
        this.setUp();
        Parser parser = new Parser();
        //-------------
        //TODO: from user to docUnit
        List<User> users = new ArrayList<>();
        //List<User> users = parser.parse(this.test_corpora_directory, "doc").getDocsAsList();
        //-------------
        setChunkId(this.test_corpora_directory.split("chunk ")[1]);
        setUsers(users);
        this._usersHashSet.addAll(this.getUsers());

        this.run(users);
    }

    private String getChunkId() {
        return chunkId;
    }

    private void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    private String getBaseUrl() {
        return baseUrl;
    }

    private void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private void run(List<User> Users) {
        this.setUsers(Users);

        if (Objects.equals(this.task, "") || this.task == null) {
            promptUserForTask();
        }

        String base_result_path = this.RESULTS_FOLDER + "/week" + getChunkId() + "/IR/tests-chunk-" + getChunkId() + "/chunk_Test_" + getChunkId();

        switch (getTask()) {
            case "mlt1":
                Scenario moreikeThis = new MoreLikeThis(this._usersHashSet, getBaseUrl(), "eriskMoreLikeThis");
                new ResultsOutput(moreikeThis.getResultsPerUsers(), base_result_path + "_mlt1.txt");
                break;
            case "mlt2":
                Scenario moreLikeThisWithStopWordsRemoval = new MoreLikeThisWithStopWordsRemoval(this._usersHashSet, getBaseUrl(), "eriskRun2");
                new ResultsOutput(moreLikeThisWithStopWordsRemoval.getResultsPerUsers(), base_result_path + "_mlt2.txt");
                break;


        }
    }

    private void setUp() {

        Properties props = Configs.getInstance().getProps();

        logger.info("Solr base URL is {}", props.getProperty("SOLR_BASE_URL"));
        setBaseUrl(props.getProperty("SOLR_BASE_URL"));

        this.test_corpora_directory = props.getProperty("TEST_CORPORA_DIRECTORY");
        this.RESULTS_FOLDER = props.getProperty("IR_RESULT_PATH");
        logger.info("Test copora path {}", props.getProperty("TEST_CORPORA_DIRECTORY"));
        logger.debug("Corpora directory = ", test_corpora_directory);
    }


    private void promptUserForTask() {
        AnsiConsole.systemInstall();
        ConsolePrompt prompt = new ConsolePrompt();
        PromptBuilder promptBuilder = prompt.getPromptBuilder();

        System.out.println(ansi().eraseScreen().render("@|green,italic AD Hoc Documents Retrieval|@ \n@|reset " +
                "Scenarios \n" +
                "1: Find comparable documents from eriskMoreLikeThis core (mlt1) \n" +
                "|@"));

        promptBuilder.createListPrompt()
                .name("adhoctask")
                .message("Which task would you execute")
                .newItem("mlt1").text("Run scenario 1 (mlt1)").add()
                .newItem("mlt2").text("Run scenario 2 (mlt2)").add()
                .newItem("with-dict").text("Run scenario 3 (with-dict)").add()
                .newItem("with-dict-and-clpsych").text("Run scenario 3 (with-dict-and-clpsych)").add()
                .addPrompt();

        try {
            HashMap<String, ? extends PromtResultItemIF> result = prompt.prompt(promptBuilder.build());
            ListResult AdHocTask = (ListResult) result.get("adhoctask");

            this.setTask(AdHocTask.getSelectedId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public List<User> getUsers() {
        return Users;
    }

    public void setUsers(List<User> users) {
        Users = users;
    }


}
