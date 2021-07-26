package skyhook;

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
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
        // Instantiate a Dataset
        FileSystemDatasetFactory factory = new FileSystemDatasetFactory(
                allocator,
                NativeMemoryPool.getDefault(),
                FileFormat.PARQUET,
                "file:///mnt/cephfs/dataset"
        );
        NativeDataset dataset = factory.finish();

        // Define the columns to read
        String[] cols = new String[0];

        // Create the ScanOptions
        ScanOptions scanOptions = new ScanOptions(cols, 1000000);

        // Create the Scanner
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
