package simple_compile;

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
        FileSystemDatasetFactory factory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(),
                FileFormat.PARQUET, "file:///users/noobjc/datasets/16MB.parquet");
        NativeDataset dataset = factory.finish();
        String[] cols = new String[0];
        NativeScanner scanner = dataset.newScan(new ScanOptions(cols, 100000));
        System.out.println(scanner.schema().toString());
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
