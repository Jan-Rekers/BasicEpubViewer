package nl.janrekers.basicepubviewer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.service.MediatypeService;

// This FileProvider gets requests for content:// URIs.
// It reads the requested data from the current epub book
public class EpubFileProvider extends ContentProvider {

    public static final Uri CONTENT_URI=Uri.parse(
            "content://" + EpubFileProvider.class.getPackageName() +"/");

    public boolean onCreate() {
        return true;
    }

    public String getType(@NonNull Uri uri) {
        // Log.i(getClass().getSimpleName(), "getType: " + uri.getPath());
        String href = uri.getPath().substring(1);
        Book book = BookKeeper.getInstance().getBook();
        if (book != null) {
            Resource resource = book.getResources().getByHref(href);
            if (resource != null)
                return resource.getMediaType().toString();
        }
        return(null);
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        // Log.i("EpubFileProvider", "openFile: " + uri.getPath());

        String href = uri.getPath().substring(1);
        Book book = BookKeeper.getInstance().getBook();
        if (book != null) {
            Resource resource = book.getResources().getByHref(href);

            if (resource.getMediaType().equals(MediatypeService.XHTML) && !resource.getId().equals( BookKeeper.getInstance().getCurrentResourceId() )) {
                // It seems that the user moved to a different section by means of an internal href; inform the bookkeeper
                BookKeeper.getInstance().setCurrentResourceId(resource.getId());
            }

            if (resource != null) {
                ParcelFileDescriptor[] pipe = null;
                try {
                    pipe = ParcelFileDescriptor.createPipe();
                    new TransferThread(
                            resource.getInputStream(),
                            new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])).start();
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), "Exception opening pipe", e);
                    throw new FileNotFoundException("Could not open pipe for: "
                            + uri.toString());
                }
                return (pipe[0]);
            }
        }
        throw new FileNotFoundException(uri.getPath());
    }

    static class TransferThread extends Thread {
        InputStream in;
        OutputStream out;

        TransferThread(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;

            try {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Exception transferring file", e);
            }
        }
    }

    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        throw new RuntimeException("Operation not supported");
    }

    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        throw new RuntimeException("Operation not supported");
    }

    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        throw new RuntimeException("Operation not supported");
    }

    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        throw new RuntimeException("Operation not supported");
    }
}
