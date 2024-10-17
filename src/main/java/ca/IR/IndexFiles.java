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

public class IndexFiles {

    private IndexFiles() {}

    public static void main(String[] args) {
        String indexPath = "index";
        String docsPath = null;
        boolean create = true;
        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                indexPath = args[i+1];
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i+1];
                i++;
            } else if ("-update".equals(args[i])) {
                create = false;
            }
        }

        if (docsPath == null) {
            System.err.println("Usage: java IndexFiles -index INDEX_PATH -docs DOCS_PATH [-update]");
            System.exit(1);
        }

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

            if (create) {
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);
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
                    } catch (IOException ignore) {}
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
                        writer.addDocument(document);
                    }

                    document = new Document();
                    String docID = currentLine.replace(".I ", "").trim();
                    document.add(new StringField("documentID", docID, Field.Store.YES));
                }

                if (currentLine.startsWith(".T")) {
                    currentLine = inputReader.readLine();
                    StringBuilder title = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        title.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Title", title.toString().trim(), Field.Store.YES));
                }

                if (currentLine.startsWith(".A")) {
                    currentLine = inputReader.readLine();
                    StringBuilder author = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        author.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Author", author.toString().trim(), Field.Store.YES));
                }

                if (currentLine.startsWith(".B")) {
                    currentLine = inputReader.readLine();
                    StringBuilder bibliography = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        bibliography.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Bibliography", bibliography.toString().trim(), Field.Store.YES));
                }

                if (currentLine.startsWith(".W")) {
                    currentLine = inputReader.readLine();
                    StringBuilder words = new StringBuilder();
                    while (currentLine != null && !currentLine.startsWith(".")) {
                        words.append(currentLine).append(" ");
                        currentLine = inputReader.readLine();
                    }
                    document.add(new TextField("Words", words.toString().trim(), Field.Store.YES));
                }
            }

            if (document != null) {
                writer.addDocument(document);
            }
        }
    }
}
