package nl.janrekers.basicepubviewer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.nio.file.Files;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

public class BookLoader extends Worker {

    public BookLoader(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String filename = getInputData().getString("FILENAME");
        try {
            File booksDir = new File(Environment.getExternalStorageDirectory(), "Books");
            File epubFile = new File(booksDir, filename);
            Book book = new EpubReader().readEpub(Files.newInputStream(epubFile.toPath()));
            BookKeeper.getInstance().loadBook(book);
        }
        catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "error loading " + filename + ": " + e);
            return Result.failure();
        }
        return Result.success();
    }
}
