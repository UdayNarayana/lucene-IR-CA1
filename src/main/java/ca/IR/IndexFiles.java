package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class IndexFiles {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: IndexFiles <indexDir> <docsDir>");
            return;
        }

        String indexPath = args[0];  // Directory to save the index
        String docsPath = args[1];   // Directory of the documents

        // Check if documents directory exists
        File docsDir = new File(docsPath);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.out.println("Document directory does not exist or is not a directory: " + docsPath);
            return;
        }

        // Open directory for index storage
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        // Index each document in the specified directory
        File[] files = docsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println("Indexing file: " + file.getName());
                    indexDoc(writer, file);
                }
            }
        } else {
            System.out.println("No files found in the directory: " + docsPath);
        }

        writer.close();
        System.out.println("Indexing completed.");
    }

    static void indexDoc(IndexWriter writer, File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }

            Document doc = new Document();
            doc.add(new TextField("contents", content.toString(), Field.Store.YES));
            doc.add(new TextField("documentID", file.getName(), Field.Store.YES));
            writer.addDocument(doc);
        }
    }
}
