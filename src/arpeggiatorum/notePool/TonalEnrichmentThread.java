package arpeggiatorum.notePool;

/**
 * @author Axel Berndt
 */
public class TonalEnrichmentThread extends Thread {
    private final NotePool notePool;
    private long referenceTime;
    protected final Object lock = new Object();

    /**
     * constructor
     * @param notePool
     */
    public TonalEnrichmentThread(NotePool notePool) {
        this.notePool = notePool;
        this.referenceTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!this.notePool.isEmpty()) {
            System.out.print(".");

            synchronized (this.lock) {
                try {
                    this.lock.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("|");
    }
}
