package nl.janrekers.basicepubviewer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.UUID;

import nl.janrekers.basicepubviewer.databinding.ActivityBookViewBinding;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;

public class BookViewActivity extends AppCompatActivity {

    private ActivityBookViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.webviewBook.getSettings().setBuiltInZoomControls(true);

        checkPermissions();

        if (BookKeeper.getInstance().getBook() == null) {
            showMessage("Please use the red bookshelf button to select a file");
        }
        else {
            binding.tvTitle.setText(BookKeeper.getInstance().getBook().getTitle());
            showPage();
        }

        binding.btnPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                movePage(-1);
            }
        });

        binding.btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                movePage(+1);
            }
        });

        binding.btnBookshelf.setOnClickListener(view -> {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(getApplicationContext(), "Failure: Need permission to read External Storage", Toast.LENGTH_LONG).show();
            }
            else {
                File internalStorage = Environment.getExternalStorageDirectory();
                File booksDir = new File(internalStorage, "Books");
                File[] allEpubFiles = booksDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".epub");
                    }
                });
                if (allEpubFiles == null || allEpubFiles.length == 0) {
                    Toast.makeText(this, "no epub files found in Books folder", Toast.LENGTH_LONG).show();
                }
                else {
                    chooseFileForLoading(allEpubFiles);
                }
            }
        });

    }


    // based on the current resId, ask the book for the next resId
    // it depends on the epub whether this is a page or an entire section
    // this is determined by the SPINE of the book
    private void movePage(int dir) {
        if (BookKeeper.getInstance().isBookIsAvailable()) {
            Book book = BookKeeper.getInstance().getBook();
            Resource curResource = book.getResources().getById(BookKeeper.getInstance().getCurrentResourceId());
            int curIndex = book.getSpine().findFirstResourceById(curResource.getId());
            if (curIndex != -1 && (curIndex + dir) >= 0 && (curIndex + dir) < book.getSpine().size()) {
                Resource nextResource = book.getSpine().getResource(curIndex + dir);
                if (nextResource != null) {
                    BookKeeper.getInstance().setCurrentResourceId(nextResource.getId());
                    showPage();
                }
            }
        }
    }

    private void checkPermissions() {
        if (!Environment.isExternalStorageManager()) {
            Snackbar snackbar = Snackbar
                    .make(binding.getRoot(), "The app needs permission to open files", Snackbar.LENGTH_INDEFINITE)
                    .setAction("go to permissions", view -> {
                        Intent intent = new Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:" + this.getClass().getPackage().getName()) );
                        storagePermissionResultLauncher.launch(intent);
                    });
            snackbar.show();
        }
    }

    ActivityResultLauncher<Intent> storagePermissionResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (Environment.isExternalStorageManager()) {
                        showMessage("Please use the red bookshelf button to select a file");
                    }
                    else {
                        showMessage("The app does not have permission to open files");
                    }
                }
            });


    private void chooseFileForLoading(File[] allEpubFiles) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Choose book");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
        for (File file: allEpubFiles)
            arrayAdapter.add(file.getName());
        b.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        b.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = arrayAdapter.getItem(which);
                loadBook(filename);
            }
        });
        b.show();
    }

    private void loadBook(String filename) {
        Data.Builder builder = new Data.Builder();
        builder.putString("FILENAME", filename);
        String tag = UUID.randomUUID().toString();
        OneTimeWorkRequest loadRequest =
                new OneTimeWorkRequest.Builder(BookLoader.class)
                        .setInputData(builder.build())
                        .addTag(tag)
                        .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(loadRequest);

        observeLoading(filename, tag);
    }

    private void observeLoading(String filename, String tag) {
        LiveData<List<WorkInfo>> workInfo = WorkManager.getInstance(getApplicationContext()).getWorkInfosByTagLiveData(tag);
        workInfo.observe(this, listOfWorkInfos -> {
            if (listOfWorkInfos == null || listOfWorkInfos.isEmpty()) return;
            WorkInfo info = listOfWorkInfos.get(0);
            if (info.getState().isFinished()) {
                binding.tvTitle.setText(BookKeeper.getInstance().getBook().getTitle());
                showPage();
            }
            else {
                showMessage("Loading: <i>" + filename + "</i>");
            }
        });
    }


    private void showPage() {
        if (BookKeeper.getInstance().isBookIsAvailable()) {
            String id = BookKeeper.getInstance().getCurrentResourceId();
            Book book = BookKeeper.getInstance().getBook();
            int indexInSpine = book.getSpine().findFirstResourceById(id);
            String href = book.getSpine().getResource(indexInSpine).getHref();
            Uri pageUri = Uri.parse(EpubFileProvider.CONTENT_URI + href);

            binding.webviewBook.loadUrl(pageUri.toString());
        }
        else {
            showMessage("...");
        }
    }


    private void showMessage(String message) {
        String htmlString =
                "<html> <body> "
                + "<div style='position: absolute; top: 50%; left: 50%; transform: translateX(-50%) translateY(-50%);'>"
                + message
                + "</body> </html>";
        binding.webviewBook.loadData(htmlString, "text/html", "UTF-8");
        binding.tvTitle.setText("");
    }


}