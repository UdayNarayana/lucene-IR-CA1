package ca.IR;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
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
    private final Analyzer analyzer;
    private final IndexWriter writer;

    public IndexFiles(String indexPath) throws IOException {
        this.analyzer = new StandardAnalyzer();
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        writer = new IndexWriter(dir, config);
    }

    public void indexDocuments(String docsPath) throws IOException {
        File docsDir = new File(docsPath);
        File[] files = docsDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println("Indexing file: " + file.getName());
                    indexDocument(file);
                }
            }
        }
        writer.close();
        System.out.println("Indexing completed.");
    }

    private void indexDocument(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder docContent = new StringBuilder();
            String docId = "";
            String title = "";

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (docContent.length() > 0) {
                        addDocument(docId, title, docContent.toString());
                        docContent.setLength(0);
                        title = "";
                    }
                    docId = line.split(" ")[1].trim();
                } else if (line.startsWith(".T")) {
                    StringBuilder titleBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(".A") || line.startsWith(".B")) {
                            break;
                        }
                        titleBuilder.append(line.trim()).append(" ");
                    }
                    title = titleBuilder.toString().trim();
                } else if (line.startsWith(".W")) {
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(".I")) {
                            docContent.append(line.trim());
                            addDocument(docId, title, docContent.toString());
                            docContent.setLength(0);
                            break;
                        }
                        docContent.append(line.trim()).append(" ");
                    }
                    if (line == null) {
                        addDocument(docId, title, docContent.toString());
                    }
                }
            }
        }
    }

    private void addDocument(String docId, String title, String textContent) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("documentID", docId, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("contents", textContent, Field.Store.YES));
        writer.addDocument(doc);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: IndexFiles <indexDir> <docsDir>");
            return;
        }

        String indexPath = args[0];
        String docsPath = args[1];

        try {
            IndexFiles indexer = new IndexFiles(indexPath);
            indexer.indexDocuments(docsPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
