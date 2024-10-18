package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Field;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;

public class IndexFiles {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java CranfieldIndexer <indexdir> <cranfile>");
            System.exit(1);
        }

        String indexPath = args[0];   // First argument: path to the index directory
        String cranFile = args[1];    // Second argument: path to cran.all.1400 file

        // Setup Lucene index
        Directory index = FSDirectory.open(Paths.get(indexPath));
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(index, config);

        // Parse cran.all.1400 file
        BufferedReader br = new BufferedReader(new FileReader(cranFile));
        String line;
        Document doc = new Document();

        while ((line = br.readLine()) != null) {
            if (line.startsWith(".I")) {
                if (doc.getFields().size() > 0) {
                    writer.addDocument(doc); // Add previous document
                }
                doc = new Document(); // New document
                doc.add(new TextField("id", line.substring(3).trim(), Field.Store.YES));
            } else if (line.startsWith(".T")) {
                doc.add(new TextField("title", br.readLine(), Field.Store.YES));
            } else if (line.startsWith(".W")) {
                StringBuilder content = new StringBuilder();
                while ((line = br.readLine()) != null && !line.startsWith(".")) {
                    content.append(line).append(" ");
                }
                doc.add(new TextField("content", content.toString(), Field.Store.YES));
            }
        }
        writer.addDocument(doc); // Add last document
        writer.close();
        br.close();

        System.out.println("Indexing completed.");
    }
}
