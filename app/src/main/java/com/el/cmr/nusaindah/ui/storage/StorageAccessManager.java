package com.el.cmr.nusaindah.ui.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Storage Access Framework Manager
 * Compliant with Google Play Console policies
 * Supports Android 6+ with modern approach
 */
public class StorageAccessManager {

    private static final String TAG = "StorageAccessManager";

    private Context context;
    private AppCompatActivity activity;
    private StorageAccessListener listener;
    private ActivityResultLauncher<Intent> folderPickerLauncher;

    public interface StorageAccessListener {
        void onFolderSelected(Uri folderUri, String folderPath);
        void onFileSaved(Uri fileUri, String fileName);
        void onStorageError(String error);
        void onPermissionDenied();
    }

    public StorageAccessManager(AppCompatActivity activity, StorageAccessListener listener) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.listener = listener;

        initializeFolderPicker();
    }

    /**
     * Initialize folder picker launcher
     */
    private void initializeFolderPicker() {
        folderPickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri folderUri = result.getData().getData();
                        if (folderUri != null) {
                            handleFolderSelection(folderUri);
                        }
                    } else {
                        listener.onPermissionDenied();
                    }
                }
        );
    }

    /**
     * Request user to select download folder using SAF
     */
    public void requestFolderAccess() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            // Set initial directory to Downloads if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                        getDownloadsTreeUri());
            }

            folderPickerLauncher.launch(intent);

        } catch (Exception e) {
            Log.e(TAG, "Failed to open folder picker", e);
            listener.onStorageError("Failed to open folder picker: " + e.getMessage());
        }
    }

    /**
     * Handle folder selection and persist permissions
     */
    private void handleFolderSelection(Uri folderUri) {
        try {
            // Take persistent permission
            activity.getContentResolver().takePersistableUriPermission(
                    folderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            // Save selected folder URI in preferences
            StoragePreferences prefs = new StoragePreferences(context);
            prefs.setSelectedFolderUri(folderUri.toString());

            // Get human-readable path
            String folderPath = getHumanReadablePath(folderUri);

            Log.d(TAG, "Folder selected: " + folderUri);
            Log.d(TAG, "Folder path: " + folderPath);

            listener.onFolderSelected(folderUri, folderPath);

        } catch (SecurityException e) {
            Log.e(TAG, "Failed to take persistent permission", e);
            listener.onStorageError("Failed to access selected folder");
        }
    }

    /**
     * Save file to user-selected folder using SAF
     */
    public boolean saveFileToSelectedFolder(InputStream inputStream, String fileName) {
        StoragePreferences prefs = new StoragePreferences(context);
        String folderUriString = prefs.getSelectedFolderUri();

        if (folderUriString == null) {
            listener.onStorageError("No folder selected. Please select a download folder first.");
            return false;
        }

        try {
            Uri folderUri = Uri.parse(folderUriString);

            // Verify we still have permission
            if (!hasValidPermission(folderUri)) {
                listener.onStorageError("Folder access expired. Please select folder again.");
                prefs.clearSelectedFolderUri();
                return false;
            }

            return saveFileToFolder(folderUri, inputStream, fileName);

        } catch (Exception e) {
            Log.e(TAG, "Failed to save file", e);
            listener.onStorageError("Failed to save file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Save file to specific folder
     */
    private boolean saveFileToFolder(Uri folderUri, InputStream inputStream, String fileName) {
        try {
            DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
            if (folder == null || !folder.exists()) {
                listener.onStorageError("Selected folder no longer exists");
                return false;
            }

            // Create app subfolder if it doesn't exist
            DocumentFile appFolder = getOrCreateAppFolder(folder);
            if (appFolder == null) {
                listener.onStorageError("Failed to create app folder");
                return false;
            }

            // Check if file already exists
            DocumentFile existingFile = appFolder.findFile(fileName);
            if (existingFile != null) {
                existingFile.delete(); // Replace existing file
            }

            // Create new file
            String mimeType = getMimeType(fileName);
            DocumentFile newFile = appFolder.createFile(mimeType, fileName);

            if (newFile == null) {
                listener.onStorageError("Failed to create file");
                return false;
            }

            // Write file content
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(newFile.getUri())) {
                if (outputStream == null) {
                    listener.onStorageError("Failed to open file for writing");
                    return false;
                }

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
            }

            listener.onFileSaved(newFile.getUri(), fileName);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "IOException while saving file", e);
            listener.onStorageError("Failed to write file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get or create app subfolder
     */
    private DocumentFile getOrCreateAppFolder(DocumentFile parentFolder) {
        String appFolderName = "MCPE Addons";

        DocumentFile appFolder = parentFolder.findFile(appFolderName);
        if (appFolder == null || !appFolder.exists()) {
            appFolder = parentFolder.createDirectory(appFolderName);
        }

        return appFolder;
    }

    /**
     * Check if we still have valid permission for the folder
     */
    private boolean hasValidPermission(Uri folderUri) {
        try {
            DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
            return folder != null && folder.exists() && folder.canWrite();
        } catch (Exception e) {
            Log.e(TAG, "Failed to check permission", e);
            return false;
        }
    }

    /**
     * Get human-readable path from URI
     */
    private String getHumanReadablePath(Uri uri) {
        try {
            DocumentFile folder = DocumentFile.fromTreeUri(context, uri);
            if (folder != null) {
                return folder.getName() != null ? folder.getName() : "Selected Folder";
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get readable path", e);
        }
        return "Selected Folder";
    }

    /**
     * Get Downloads tree URI for Android O+
     */
    private Uri getDownloadsTreeUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload");
        }
        return null;
    }

    /**
     * Get MIME type for file
     */
    private String getMimeType(String fileName) {
        String lowerCase = fileName.toLowerCase();
        if (lowerCase.endsWith(".mcaddon") || lowerCase.endsWith(".mcpack") ||
                lowerCase.endsWith(".mcworld")) {
            return "application/octet-stream";
        } else if (lowerCase.endsWith(".zip")) {
            return "application/zip";
        }
        return "application/octet-stream";
    }

    /**
     * Check if folder is selected
     */
    public boolean isFolderSelected() {
        StoragePreferences prefs = new StoragePreferences(context);
        String folderUri = prefs.getSelectedFolderUri();
        return folderUri != null && hasValidPermission(Uri.parse(folderUri));
    }

    /**
     * Get selected folder info
     */
    public String getSelectedFolderInfo() {
        StoragePreferences prefs = new StoragePreferences(context);
        String folderUriString = prefs.getSelectedFolderUri();

        if (folderUriString != null) {
            try {
                Uri folderUri = Uri.parse(folderUriString);
                return getHumanReadablePath(folderUri);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get folder info", e);
            }
        }

        return "No folder selected";
    }

    /**
     * Clear selected folder
     */
    public void clearSelectedFolder() {
        StoragePreferences prefs = new StoragePreferences(context);
        prefs.clearSelectedFolderUri();
    }
}