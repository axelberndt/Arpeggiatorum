//package arpeggiatorum.notePool;
//
//import arpeggiatorum.gui.GUI;
//
///**
// * @author Axel Berndt
// */
//public class TonalEnrichmentThread extends Thread {
//    private final NotePool notePool;
//    private long referenceTime;
//    protected final Object lock = new Object();
//
//    /**
//     * constructor
//     * @param notePool
//     */
//    public TonalEnrichmentThread(NotePool notePool) {
//        this.notePool = notePool;
//        this.referenceTime = System.currentTimeMillis();
//    }
//
//    @Override
//    public void run() {
//        while (!this.notePool.isEmpty()) {
//            //System.out.print(".");
//            GUI.updateLogGUI(".");
//            synchronized (this.lock) {
//                try {
//                    this.lock.wait(50);
//                } catch (InterruptedException e) {
//                   // //e.printStackTrace();
//                    GUI.updateLogGUI(e.getMessage());
//                }
//            }
//        }
//        //System.out.println("|");
//        GUI.updateLogGUI("|");
//    }
//}