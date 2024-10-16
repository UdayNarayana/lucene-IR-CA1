package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
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
        String indexPath = args[0];
        String docsPath = args[1];

        Directory dir = FSDirectory.open(Paths.get(indexPath));
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        File docsDir = new File(docsPath);
        for (File file : docsDir.listFiles()) {
            System.out.println("Indexing file: " + file.getName());
            indexDoc(writer, file);
        }
        writer.close();
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
            doc.add(new TextField("filename", file.getName(), Field.Store.YES));
            writer.addDocument(doc);
        }
    }
}
