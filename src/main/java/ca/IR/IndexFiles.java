package ca.IR;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
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

    private static EnglishAnalyzer analyzer = new EnglishAnalyzer();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: IndexFiles <indexDir> <docsDir>");
            return;
        }

        String indexPath = args[0];
        String docsPath = args[1];

        File docsDir = new File(docsPath);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.out.println("Document directory does not exist or is not a directory: " + docsPath);
            return;
        }

        Directory dir = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = new IndexWriter(dir, config)) {
            File[] files = docsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        System.out.println("Indexing file: " + file.getName());
                        indexDocuments(writer, file);
                    }
                }
            } else {
                System.out.println("No files found in the directory: " + docsPath);
            }
        }

        System.out.println("Indexing completed.");
    }

    public static void indexDocuments(IndexWriter writer, File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder docContent = new StringBuilder();
            int docId = 0;
            String title = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (docContent.length() > 0) {
                        addDocument(writer, docId, title, docContent.toString());
                        docContent.setLength(0);
                        title = "";
                    }
                    docId = Integer.parseInt(line.split(" ")[1].trim());
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
                    StringBuilder contentBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(".I")) {
                            docContent.append(contentBuilder.toString().trim());
                            addDocument(writer, docId, title, docContent.toString());
                            docContent.setLength(0);
                            docId = Integer.parseInt(line.split(" ")[1].trim());
                            break;
                        }
                        contentBuilder.append(line.trim()).append(" ");
                    }
                    if (line == null) {
                        docContent.append(contentBuilder.toString().trim());
                        addDocument(writer, docId, title, docContent.toString());
                    }
                }
            }
        }
    }

    private static void addDocument(
            IndexWriter writer,
            int docID,
            String title,
            String textContent
    ) throws IOException {
        Document doc = new Document();
        doc.add(new StoredField("documentID", docID));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("contents", textContent, Field.Store.YES));
        writer.addDocument(doc);
    }
}
