package skyhook;

import org.apache.arrow.dataset.file.JniWrapper;
import org.apache.arrow.dataset.jni.*;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.dataset.file.*;
import org.apache.arrow.dataset.scanner.*;

import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


class ArrowDatasetScanTask implements Runnable {
    private NativeScanTask scanTask;
    public ArrowDatasetScanTask(NativeScanTask scanTask) {
        this.scanTask = scanTask;
    }

    public NativeScanTask getScanTask() {
        return this.scanTask;
    }

    public void run() {
        try {
            ScanTask.BatchIterator itr = this.scanTask.execute();
            while (itr.hasNext()) {
                System.out.println(itr.next().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class App {
    public static void main(String[] args) {
        // Instatiate the Dataset
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
        NativeMemoryPool memoryPool = NativeMemoryPool.getDefault();
        NativeDatasetFactory factory = new NativeDatasetFactory(allocator, memoryPool, JniWrapper.get().makeFileSystemDatasetFactory("file:///mnt/cephfs/dataset", 0));
        NativeDataset dataset = factory.finish();
        factory.close();

        // Define the columns to read and Create the Scanner
        String[] cols = new String[0];
        ScanOptions scanOptions = new ScanOptions(cols, 1000000);
        NativeScanner scanner = dataset.newScan(scanOptions);

        // Launch a parallel scan
        launchScan(scanner);
    }

    public static void launchScan(NativeScanner scanner) {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(16);
        Iterator<NativeScanTask> itr = (Iterator<NativeScanTask>) scanner.scan().iterator();
        while(itr.hasNext()){
            ArrowDatasetScanTask arrowDatasetScanTask = new ArrowDatasetScanTask(itr.next());
            threadPoolExecutor.execute(arrowDatasetScanTask);
        }
        threadPoolExecutor.shutdown();
    }
}
