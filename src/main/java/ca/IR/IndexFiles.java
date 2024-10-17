package ca.IR;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Scanner;

public class IndexFiles {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt user for index and document paths
        System.out.print("Enter the path to the index directory: ");
        String indexPath = scanner.nextLine(); // Index directory path input

        System.out.print("Enter the path to the documents directory: ");
        String docsPath = scanner.nextLine(); // Documents directory path input

        boolean create = true; // Always create a new index

        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" + docDir.toAbsolutePath() + "' does not exist or is not readable.");
            System.exit(1);
        }

        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            iwc.setOpenMode(OpenMode.CREATE); // Always create a new index

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir); // Indexing the documents
            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String currentLine = inputReader.readLine();
            Document document = null;

            while (currentLine != null) {
                if (currentLine.startsWith(".I")) {
                    if (document != null) {
                        writer.addDocument(document); // Add document to index
                    }
                    document = new Document();
                    String docID = currentLine.replace(".I ", "").trim(); // Extract document ID
                    document.add(new StringField("documentID", docID, Field.Store.YES)); // Store document ID
                }

                if (currentLine.startsWith(".T")) {
                    currentLine = inputReader.readLine();
                    StringBuilder title = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        title.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Title", title.toString().trim(), Field.Store.YES)); // Store Title
                }

                if (currentLine.startsWith(".A")) {
                    currentLine = inputReader.readLine();
                    StringBuilder author = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        author.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Author", author.toString().trim(), Field.Store.YES)); // Store Author
                }

                if (currentLine.startsWith(".B")) {
                    currentLine = inputReader.readLine();
                    StringBuilder bibliography = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        bibliography.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Bibliography", bibliography.toString().trim(), Field.Store.YES)); // Store Bibliography
                }

                if (currentLine.startsWith(".W")) {
                    currentLine = inputReader.readLine();
                    StringBuilder words = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        words.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Words", words.toString().trim(), Field.Store.YES)); // Store Words
                }
            }

            if (document != null) {
                writer.addDocument(document); // Add the last document to the index
            }
        }
    }
}
