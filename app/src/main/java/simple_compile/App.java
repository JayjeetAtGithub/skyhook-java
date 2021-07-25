package simple_compile;

import org.apache.arrow.dataset.jni.*;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.dataset.file.*;
import org.apache.arrow.dataset.scanner.*;
import org.apache.arrow.dataset.source.*;


class App {
    public static void main(String[] args) {
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
        FileSystemDatasetFactory factory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(),
                FileFormat.PARQUET, "file:///path/to/a/sample.paret");
        NativeDataset dataset = factory.finish();
        String[] cols = new String[0];
        NativeScanner scanner = dataset.newScan(new ScanOptions(cols, 100000));
        System.out.println(scanner.schema().toString());
    }
}
