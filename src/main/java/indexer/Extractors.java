package indexer;

import common.Configs;
import common.User;
import extractor.DictionaryExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class Extractors {
    private final String DRUGS_FILE_NAME = "drugs.txt";
    private final String MEDS_FILE_NAME = "meds.txt";
    private final String DISEASES_FILE_NAME = "mental-disorders.txt";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Boolean extractDrugs;
    private Boolean extractDiseases;
    private Boolean extractMeds;
    private Properties props = Configs.getInstance().getProps();
    private final String resourcesDirectory = props.getProperty("RESOURCES_DIR");
    private final String workDirectory = props.getProperty("WORK_DIRECTORY");
    private final String dictionariesDirectory = workDirectory + resourcesDirectory + props.getProperty("DICTIONARIES_DIR") + "/";
    private List<User> users;

    public Extractors(List<User> users) {
        this.users = users;
    }

    public Extractors() {

    }

    public void handleExtracting() {
        if (extractDrugs) extractDrugs();
        if (extractMeds) extractMeds();
        if (extractMeds) extractMeds();
        if (extractDiseases) extractDiseases();
    }

    private void extractDrugs() {
        String pathToDrugs = this.dictionariesDirectory + DRUGS_FILE_NAME;
        ArrayList<String> dictionary = loadDictionary(pathToDrugs);
        extractFeature(dictionary, "drugs");

    }

    private void extractDiseases() {
        String pathToDiseases = this.dictionariesDirectory + DISEASES_FILE_NAME;
        ArrayList<String> dictionary = loadDictionary(pathToDiseases);
        extractFeature(dictionary, "diseases");

    }

    private void extractMeds() {
        String pathToMeds = this.dictionariesDirectory + MEDS_FILE_NAME;
        ArrayList<String> medsDictionnary = loadDictionary(pathToMeds);
        extractFeature(medsDictionnary, "meds");
    }

    private void extractFeature(ArrayList<String> dictionary, String featureName) {
        logger.debug("Extracting {}", featureName);
        users.forEach(user -> {
            user.getPosts().getPosts().forEach(post -> {
                ArrayList<String> _feature = new ArrayList<>();

                dictionary.forEach(feature -> {
                    if (feature.length() > 0 && post.getContent().length() > 0) {
                        if (post.getContent().matches(".*\\b" + feature + "\\b.*") && !_feature.contains(feature)) {
                            _feature.add(feature);
                        }
                    }
                });

                if (featureName.equals("diseases")) {
                    post.setDiseases(_feature);
                }
                if (featureName.equals("drugs")) {
                    post.setDrugs(_feature);
                }
                if (featureName.equals("meds")) {
                    post.setMeds(_feature);
                }
            });

        });
    }

    public void setExtractors() {
        if (System.getProperty("indexer:extract:drugs") == null) {
            this.extractDrugs = false;
        } else {
            this.extractDrugs = Boolean.valueOf(System.getProperty("indexer:extract:drugs"));
        }

        if (System.getProperty("indexer:extract:meds") == null) {
            this.extractMeds = false;
        } else {
            this.extractMeds = Boolean.valueOf(System.getProperty("indexer:extract:meds"));
        }

        if (System.getProperty("indexer:extract:diseases") == null) {
            this.extractDiseases = false;
        } else {
            this.extractDiseases = Boolean.valueOf(System.getProperty("indexer:extract:diseases"));
        }
    }

    ArrayList<String> loadDictionary(String dictionaryFile) {
        ArrayList<String> dictionary = new ArrayList<>();
        DictionaryExtractor extractor = new DictionaryExtractor();
        try (Stream<String> stream = Files.lines(Paths.get(dictionaryFile))) {
            stream.forEach((String line) -> {
                if (line.charAt(0) != '#') {
                    dictionary.add(extractor.normalizeSingleWord(line));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dictionary;
    }

    Boolean getExtractDrugs() {
        return extractDrugs;
    }

    Boolean getExtractMeds() {
        return extractMeds;
    }

    public List<User> getUsers() {
        return users;
    }
}
