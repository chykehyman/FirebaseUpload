package com.andela.android.firebaseupload;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_select_file)
    Button selectFileBtn;

    @BindView(R.id.button_start_upload)
    Button uploadFileBtn;

    @BindView(R.id.button_cancel_upload)
    Button cancelUploadBtn;

    @BindView(R.id.text_filename_label)
    TextView fileNameText;

    @BindView(R.id.text_upload_label)
    TextView uploadLabel;

    @BindView(R.id.text_size_label)
    TextView uploadSizeProgress;

    @BindView(R.id.text_progress_label)
    TextView uploadPercentProgress;

    @BindView(R.id.bar_upload_progress)
    ProgressBar uploadProgressBar;

    @BindView(R.id.imageView)
    ImageView imagePreview;

    @BindView(R.id.constraint_second_layout)
    ConstraintLayout uploadActivityLayout;

    public Uri filePath;
    private static final int SELECT_FILE_REQUEST_CODE = 712;
    private static final String FB_STORAGE_PATH = "files/";

    StorageReference storageReference;
    StorageTask storageTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        storageReference = FirebaseStorage.getInstance().getReference();

        selectFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileSelector();
            }
        });

        uploadFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("Upload File".equals(uploadFileBtn.getText())) {
                    setUploadActivityVisibility(true);
                    uploadFile();
                    uploadFileBtn.setText(R.string.pause_upload);
                } else if ("Pause Upload".equals(uploadFileBtn.getText())) {
                    uploadFileBtn.setText(R.string.resume_upload);
                    storageTask.pause();
                } else if ("Resume Upload".equals(uploadFileBtn.getText())) {
                    uploadFileBtn.setText(R.string.pause_upload);
                    storageTask.resume();
                }
            }
        });

        cancelUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storageTask.cancel();
                setUploadActivityVisibility(false);
            }
        });
    }

    private void openFileSelector() {
        imagePreview.setVisibility(View.GONE);
        uploadActivityLayout.setVisibility(View.GONE);
        setUploadActivityVisibility(false);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent
                            .createChooser(intent, "Select file"),
                    SELECT_FILE_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Please install a file manager",
                    Toast.LENGTH_SHORT)
                    .show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_FILE_REQUEST_CODE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {

            filePath = data.getData();

            String fileName = getFileName(filePath);

            if ("jpg".equals(getFileExtension(filePath)) ||
                    "png".equals(getFileExtension(filePath)) ||
                    "gif".equals(getFileExtension(filePath))) {

                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imagePreview.setVisibility(View.VISIBLE);
                imagePreview.setImageBitmap(bitmap);
            } else {
                if (imagePreview.getVisibility() == View.VISIBLE) {
                    imagePreview.setVisibility(View.GONE);
                }
            }
            uploadActivityLayout.setVisibility(View.VISIBLE);
            fileNameText.setText(fileName);

        } else {
            if (imagePreview.getVisibility() == View.VISIBLE) {
                imagePreview.setVisibility(View.GONE);
            }
            if (uploadActivityLayout.getVisibility() == View.VISIBLE) {
                uploadActivityLayout.setVisibility(View.GONE);
            }

            setUploadActivityVisibility(false);

            Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private String getFileName(Uri uri) {
        File myFile = new File(uri.toString());
        String fileDisplayName = null;

        if (uri.toString().startsWith("content://")) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    fileDisplayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                assert cursor != null;
                cursor.close();
            }
        } else {
            if (uri.toString().startsWith("file://")) {
                fileDisplayName = myFile.getName();
            }
        }


        return fileDisplayName;
    }

    public void setUploadActivityVisibility(boolean isUploading) {
        if (isUploading) {
            uploadLabel.setText(R.string.uploading_text);
            uploadProgressBar.setVisibility(View.VISIBLE);
            uploadSizeProgress.setVisibility(View.VISIBLE);
            uploadPercentProgress.setVisibility(View.VISIBLE);
            cancelUploadBtn.setVisibility(View.VISIBLE);
        } else {
            uploadLabel.setText(R.string.file_name);
            uploadFileBtn.setText(R.string.upload_file);
            uploadProgressBar.setVisibility(View.GONE);
            uploadSizeProgress.setVisibility(View.GONE);
            uploadPercentProgress.setVisibility(View.GONE);
            cancelUploadBtn.setVisibility(View.GONE);
        }

    }

    private void uploadFile() {
        if (filePath != null) {

            StorageReference ref = storageReference
                    .child(FB_STORAGE_PATH + getFileName(filePath));

            storageTask = ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            uploadLabel.setText(R.string.sucess_upload_text);
                            uploadFileBtn.setText(R.string.upload_file);
                            cancelUploadBtn.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Display toast message
                            Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            // show upload progress
                            double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            uploadProgressBar.setProgress((int) progress);

                            String progressText = taskSnapshot.getBytesTransferred() / (1024 * 1024) +
                                    " / " + taskSnapshot.getTotalByteCount() / (1024 * 1024) + " mb";

                            uploadSizeProgress.setText(progressText);

                            uploadPercentProgress.setText((int) progress + "%");

                        }
                    });
        } else {
            Toast.makeText(MainActivity.this, "Please select an image", Toast.LENGTH_SHORT).show();
        }
    }

}
