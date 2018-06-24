package indexer;

/**
 *
 * @author ludovic
 */
public class IndexerStatus {

    private long nbDocsToProcess;
    private long nbProcessed;
    private long nbErrors;
    private long nbWarnings;

    public IndexerStatus() {
    }

    public long getNbProcessed() {
        return nbProcessed;
    }

    public void setNbProcessed(long nbProcessed) {
        this.nbProcessed = nbProcessed;
    }

    public long getNbDocsToProcess() {
        return nbDocsToProcess;
    }

    public void setNbDocsToProcess(long nbDocsToProcess) {
        this.nbDocsToProcess = nbDocsToProcess;
    }

    public long getNbErrors() {
        return nbErrors;
    }

    public void setNbErrors(long nbErrors) {
        this.nbErrors = nbErrors;
    }

    public long getNbWarnings() {
        return nbWarnings;
    }

    public void setNbWarnings(long nbWarnings) {
        this.nbWarnings = nbWarnings;
    }   
    

    public void incrementNbDocsToProcess(long count) {
        this.nbDocsToProcess += count;
    }
    
    public void incrementNbDocsToProcess() {
        this.nbDocsToProcess += 1;
    }
    
    public void incrementNbProcessedDocs() {
        this.nbProcessed += 1;
    }
    
    public void incrementErrors(){
        this.nbErrors += 1;
    }
    
    public void incrementWarnings(){
        this.nbWarnings += 1;
    }

}
