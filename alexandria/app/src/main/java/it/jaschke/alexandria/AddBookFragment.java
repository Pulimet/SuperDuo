package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBookFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText editText;
    private Button btnScan, btnSave, btnDelete;
    private TextView tvBookTitle, tvBookSubTitle, tvAuthors, tvCategories;
    private ImageView imgBookCover;
    private final int LOADER_ID = 1;
    private View rootView;
    private BarcodeDetector detector;
    private final String EAN_CONTENT = "eanContent";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public AddBookFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (editText != null) {
            outState.putString(EAN_CONTENT, editText.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        setViews(rootView);


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                onEditTextChange(s.toString());
            }
        });
        btnScan.setOnClickListener(clickListener);
        btnSave.setOnClickListener(clickListener);
        btnDelete.setOnClickListener(clickListener);

        if (savedInstanceState != null) {
            editText.setText(savedInstanceState.getString(EAN_CONTENT));
            editText.setHint("");
        }

        initAndCheckDetector();
        checkCamera();
        return rootView;
    }

    private void setViews(View rootView) {
        editText = (EditText) rootView.findViewById(R.id.ean);
        btnScan = (Button) rootView.findViewById(R.id.scan_button);
        btnSave = (Button) rootView.findViewById(R.id.save_button);
        btnDelete = (Button) rootView.findViewById(R.id.delete_button);
        tvBookTitle = (TextView) rootView.findViewById(R.id.bookTitle);
        tvBookSubTitle = (TextView) rootView.findViewById(R.id.bookSubTitle);
        tvAuthors = (TextView) rootView.findViewById(R.id.authors);
        tvCategories = (TextView) rootView.findViewById(R.id.categories);
        imgBookCover = (ImageView) rootView.findViewById(R.id.bookCover);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.scan_button:
                    getAndRecognizeBarcode();
                    break;
                case R.id.save_button:
                    editText.setText("");
                    break;
                case R.id.delete_button:
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, editText.getText().toString());
                    bookIntent.setAction(BookService.DELETE_BOOK);
                    getActivity().startService(bookIntent);
                    editText.setText("");
                    break;
            }
        }
    };

    private void showToast(String msg) {
        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void initAndCheckDetector() {
        detector = new BarcodeDetector.Builder(getActivity().getApplicationContext())
                .setBarcodeFormats(Barcode.EAN_13)
                .build();
        if (!detector.isOperational()) {
            showToast(getResources().getString(R.string.toast_detector_fail));
            detector = null;
        }
    }

    private void checkCamera() {
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            showToast(getResources().getString(R.string.toast_no_camera));
        }
    }

    private void getAndRecognizeBarcode() {
        if (detector != null) {
            Uri fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
            ShPref.setFileUri(getActivity().getApplicationContext(), fileUri.toString());
            Log.d("ZAQ", "fileUri.toString(): " + fileUri.toString());
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
            // start the image capture Intent
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                recognizeAndUpdateIsbn();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User cancelled the image capture
                showToast(getResources().getString(R.string.toast_try_again));
            } else {
                // Image capture failed, advise user
                showToast(getResources().getString(R.string.toast_try_again));
            }
        }
    }

    private void recognizeAndUpdateIsbn() {
        Bitmap myBitmap = getBitmap();

        if (myBitmap != null) {
            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(frame);

            if (barcodes.size() > 0) {
                Barcode thisCode = barcodes.valueAt(0);
                editText.setText(thisCode.rawValue);
            } else {
                showToast(getResources().getString(R.string.toast_not_recognized));
            }
        } else {
            showToast(getResources().getString(R.string.toast_try_again) + " (bitmap  = null)");
        }

    }

    private Bitmap getBitmap() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            AssetFileDescriptor fileDescriptor;
            String fileUriStr = ShPref.getFileUri(getActivity().getApplicationContext());
            Log.d("ZAQ", "fileUriStr: " + fileUriStr);
            Uri.Builder uriBuilder = Uri.parse(fileUriStr).buildUpon();
            Uri fileUri = uriBuilder.build();
            if (fileUri != null) {
                Log.d("ZAQ", "fileUri.toString(): " + fileUri.toString());
                fileDescriptor = getActivity().getContentResolver().openAssetFileDescriptor(fileUri, "r");
            } else {
                Log.d("ZAQ", "fileUri: null");
                return null;
            }
            if (fileDescriptor == null){
                Log.d("ZAQ", "fileDescriptor == null");
                return null;
            }
            BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
            int imageHeight = options.outHeight;
            int imageWidth = options.outWidth;
            String imageType = options.outMimeType;
            int sampleSize = 1;
            while (imageHeight > 3000 || imageWidth > 3000) {
                imageHeight /= 2;
                imageWidth /= 2;
                sampleSize++;
            }

            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
        } catch (IOException e) {
            e.printStackTrace();
            showToast(getResources().getString(R.string.toast_try_again));
            Log.d("ZAQ", "IOException");
        }
        return null;
    }

    private void onEditTextChange(String isbnString) {
        Log.d("ZAQ", "onEditTextChange() isbnString: " + isbnString);

        int len = isbnString.length();
        if (len == 10 && !isbnString.startsWith("978")) {
            isbnString = "978" + isbnString;
        } else if (len < 13) {
            clearFields();
            return;
        }

        //Once we have an ISBN, start a book intent
        startFetchBookIntent(isbnString);
    }

    private void startFetchBookIntent(String isbnString) {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, isbnString);
        bookIntent.setAction(BookService.FETCH_BOOK);
        getActivity().startService(bookIntent);
        AddBookFragment.this.restartLoader();
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (editText.getText().length() == 0) {
            return null;
        }
        String eanStr = editText.getText().toString();
        if (eanStr.length() == 10 && !eanStr.startsWith("978")) {
            eanStr = "978" + eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        tvBookTitle.setText(data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE)));
        tvBookSubTitle.setText(data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE)));
        tvCategories.setText(data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY)));

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        if (authors != null) {
            String[] authorsArr = authors.split(",");
            tvAuthors.setLines(authorsArr.length);
            tvAuthors.setText(authors.replace(",", "\n"));
        }
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            new DownloadImage(imgBookCover).execute(imgUrl);
            imgBookCover.setVisibility(View.VISIBLE);
        }

        btnSave.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields() {
        tvBookTitle.setText("");
        tvBookSubTitle.setText("");
        tvAuthors.setText("");
        tvCategories.setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        btnSave.setVisibility(View.INVISIBLE);
        btnDelete.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
