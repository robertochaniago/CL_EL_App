package com.el.cmr.nusaindah.ui.download;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.el.cmr.nusaindah.databinding.DialogDownloadProgressBinding;
import com.el.cmr.nusaindah.databinding.DialogDownloadSuccessBinding;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;

/**
 * Modern Firebase Download Manager
 * - Google Play Console Compliant
 * - Support Android 6+
 * - Modern UI with Animations
 * - Minimal Permissions
 */
public class FirebaseDownloadManager {

    private static final String TAG = "FirebaseDownloadManager";
    private static final String APP_FOLDER = "addons";

    private Activity activity;
    private FirebaseStorage storage;
    private Dialog progressDialog;
    private Dialog successDialog;
    private boolean isDownloading = false;

    // UI Bindings
    private DialogDownloadProgressBinding progressBinding;
    private DialogDownloadSuccessBinding successBinding;

    // Download Statistics
    private long totalBytes = 0;
    private long downloadedBytes = 0;
    private long startTime;
    private String fileName;
    private File downloadedFile;

    public interface DownloadCallback {
        void onDownloadStart();
        void onProgress(int progress, long downloaded, long total, String speed);
        void onSuccess(String filePath);
        void onError(String error);
        void onCancelled();
    }

    public FirebaseDownloadManager(Activity activity) {
        this.activity = activity;
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Start download with modern UI
     */
    public void startDownload(String firebaseStoragePath, String fileName, DownloadCallback callback) {
        if (isDownloading) {
            Toast.makeText(activity, "Download already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initially use database name as fallback
        this.fileName = ensureProperExtension(fileName);

        Log.d(TAG, "Starting download: " + firebaseStoragePath);
        Log.d(TAG, "Database file name: " + fileName);

        StorageReference storageRef = storage.getReference().child(firebaseStoragePath);

        // Show progress dialog with temporary name
        showProgressDialog();

        // Get real file name from Firebase Storage metadata
        storageRef.getMetadata().addOnSuccessListener(metadata -> {
            totalBytes = metadata.getSizeBytes();

            // IMPORTANT: Use real file name from Firebase Storage
            String realFirebaseFileName = metadata.getName();
            if (realFirebaseFileName != null && !realFirebaseFileName.isEmpty()) {
                this.fileName = realFirebaseFileName;
                Log.d(TAG, "✅ Real Firebase file name: " + this.fileName);

                // Update UI dengan nama file asli dari Firebase Storage
                if (progressBinding != null) {
                    progressBinding.tvFileName.setText(this.fileName);
                }
            } else {
                // Fallback: extract from path
                this.fileName = extractFileNameFromPath(firebaseStoragePath);
                Log.d(TAG, "📁 Extracted from path: " + this.fileName);

                if (progressBinding != null) {
                    progressBinding.tvFileName.setText(this.fileName);
                }
            }

            updateProgress(0, "Preparing download...");

            startTime = System.currentTimeMillis();
            isDownloading = true;
            callback.onDownloadStart();

            // Start actual download
            storageRef.getStream().addOnSuccessListener(taskSnapshot -> {
                new Thread(() -> processDownload(taskSnapshot.getStream(), callback)).start();

            }).addOnFailureListener(exception -> {
                isDownloading = false;
                hideProgressDialog();
                //String errorMsg = getFirebaseErrorMessage(exception);
                //callback.onError("Download failed: " + errorMsg);
                Log.e(TAG, "Download stream failed", exception);
            });

        }).addOnFailureListener(exception -> {
            isDownloading = false;
            hideProgressDialog();
            //String errorMsg = getFirebaseErrorMessage(exception);
            //callback.onError("Failed to get file info: " + errorMsg);
            Log.e(TAG, "Metadata fetch failed", exception);
        });
    }

    /**
     * Extract file name from Firebase Storage path
     */
    private String extractFileNameFromPath(String firebaseStoragePath) {
        if (firebaseStoragePath == null || firebaseStoragePath.isEmpty()) {
            return "addon_file.mcaddon";
        }

        // Get file name after last slash
        String[] pathParts = firebaseStoragePath.split("/");
        if (pathParts.length > 0) {
            String extractedName = pathParts[pathParts.length - 1];
            if (!extractedName.isEmpty()) {
                return extractedName;
            }
        }

        return "addon_file.mcaddon";
    }

    /**
     * Process download with real-time updates
     */
    private void processDownload(InputStream inputStream, DownloadCallback callback) {
        try {
            File downloadFile = createDownloadFile();
            FileOutputStream outputStream = new FileOutputStream(downloadFile);

            byte[] buffer = new byte[8192]; // 8KB buffer for smooth progress
            int bytesRead;
            downloadedBytes = 0;

            Handler mainHandler = new Handler(Looper.getMainLooper());
            long lastUpdateTime = System.currentTimeMillis();
            long lastBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1 && isDownloading) {
                outputStream.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                // Update progress every 250ms for smooth animation
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime > 250) {
                    // Calculate download speed
                    long timeDiff = currentTime - lastUpdateTime;
                    long bytesDiff = downloadedBytes - lastBytes;
                    double speedBytesPerSec = (double) bytesDiff / (timeDiff / 1000.0);
                    String speed = formatSpeed(speedBytesPerSec);

                    int progress = (int) ((downloadedBytes * 100) / totalBytes);

                    mainHandler.post(() -> {
                        updateProgress(progress, speed);
                        callback.onProgress(progress, downloadedBytes, totalBytes, speed);
                    });

                    lastUpdateTime = currentTime;
                    lastBytes = downloadedBytes;
                }
            }

            outputStream.close();
            inputStream.close();

            if (isDownloading && downloadedBytes == totalBytes) {
                downloadedFile = downloadFile;

                // Add to MediaStore for Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    addToMediaStore(downloadFile);
                }

                mainHandler.post(() -> {
                    isDownloading = false;
                    hideProgressDialog();
                    showSuccessDialog();
                    callback.onSuccess(downloadFile.getAbsolutePath());
                });
            } else if (!isDownloading) {
                // Download cancelled
                downloadFile.delete();
                mainHandler.post(() -> callback.onCancelled());
            }

        } catch (Exception e) {
            isDownloading = false;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                hideProgressDialog();
                callback.onError("Download failed: " + e.getMessage());
            });
            Log.e(TAG, "Download processing failed", e);
        }
    }

    /**
     * Create download file in safe location dengan real Firebase file name
     */
    private File createDownloadFile() {
        File downloadDir;

//         Use Downloads folder - no permissions needed for Android 6+
//        downloadDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DOWNLOADS), APP_FOLDER);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use scoped storage
            downloadDir = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APP_FOLDER);
        } else {
            // Android 7-9 - Use app-specific external directory (no permissions needed)
            downloadDir = new File(activity.getExternalFilesDir(null), APP_FOLDER);
        }

        if (!downloadDir.exists()) {
            boolean created = downloadDir.mkdirs();
            Log.d(TAG, "Download directory created: " + created);
        }

        // Use real Firebase file name untuk naming file
        Log.d(TAG, "Creating file with real Firebase name: " + this.fileName);
        return new File(downloadDir, this.fileName);
    }

    /**
     * Add to MediaStore for Android 10+ (Optional)
     */
    private void addToMediaStore(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
                values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/" + APP_FOLDER);

                activity.getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            } catch (Exception e) {
                Log.w(TAG, "Failed to add to MediaStore", e);
            }
        }
    }

    /**
     * Show modern progress dialog with animations
     */
    private void showProgressDialog() {
        progressBinding = DialogDownloadProgressBinding.inflate(LayoutInflater.from(activity));

        progressDialog = new Dialog(activity);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(progressBinding.getRoot());
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set dialog size untuk memastikan tidak terpotong
        Window window = progressDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9); // 90% dari lebar screen
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Set initial values
        progressBinding.tvFileName.setText(fileName);
        progressBinding.progressDownload.setProgress(0);
        progressBinding.tvProgressPercentage.setText("0%");
        progressBinding.tvDownloadSize.setText("0 MB / 0 MB");
        progressBinding.tvDownloadSpeed.setText("Speed: Preparing...");

        // Setup cancel button
        progressBinding.btnCancelDownload.setOnClickListener(v -> cancelDownload());

        progressDialog.show();

        // Entrance animation
        progressBinding.getRoot().setAlpha(0f);
        progressBinding.getRoot().setScaleX(0.8f);
        progressBinding.getRoot().setScaleY(0.8f);

        progressBinding.getRoot().animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Update progress with smooth animations
     */
    private void updateProgress(int progress, String speed) {
        if (progressBinding == null) return;

        // Animate progress bar
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(
                progressBinding.progressDownload, "progress",
                progressBinding.progressDownload.getProgress(), progress);
        progressAnimator.setDuration(250);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.start();

        // Update text with animation
        progressBinding.tvProgressPercentage.setText(progress + "%");

        String downloaded = formatFileSize(downloadedBytes);
        String total = formatFileSize(totalBytes);
        progressBinding.tvDownloadSize.setText(downloaded + " / " + total);
        progressBinding.tvDownloadSpeed.setText("Speed: " + speed);

        // Pulse animation for percentage
        progressBinding.tvProgressPercentage.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() ->
                        progressBinding.tvProgressPercentage.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start())
                .start();
    }

    /**
     * Cancel download with animation
     */
    private void cancelDownload() {
        isDownloading = false;

        // Exit animation
        progressBinding.getRoot().animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                })
                .start();

        Toast.makeText(activity, "Download cancelled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Hide progress dialog with animation
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressBinding.getRoot().animate()
                    .alpha(0f)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(200)
                    .withEndAction(() -> progressDialog.dismiss())
                    .start();
        }
    }

    /**
     * Show success dialog with animation
     */
    private void showSuccessDialog() {
        successBinding = DialogDownloadSuccessBinding.inflate(LayoutInflater.from(activity));

        successDialog = new Dialog(activity);
        successDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        successDialog.setContentView(successBinding.getRoot());
        successDialog.setCancelable(true);
        successDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set dialog size untuk memastikan tidak terpotong
        Window window = successDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9); // 90% dari lebar screen
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Set file information dengan real Firebase file name
        successBinding.tvSuccessFileName.setText(this.fileName); // Real Firebase file name
        successBinding.tvFilePath.setText(downloadedFile.getAbsolutePath());

        Log.d(TAG, "Success dialog - Real file name: " + this.fileName);
        Log.d(TAG, "Success dialog - File path: " + downloadedFile.getAbsolutePath());

        // Setup button listeners
        successBinding.btnOpenFolder.setOnClickListener(v -> openDownloadFolder());
        successBinding.btnCloseSuccess.setOnClickListener(v -> {
            // Exit animation for success dialog
            successBinding.getRoot().animate()
                    .alpha(0f)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(200)
                    .withEndAction(() -> successDialog.dismiss())
                    .start();
        });

        successDialog.show();

        // Entrance animation for success dialog
        successBinding.getRoot().setAlpha(0f);
        successBinding.getRoot().setScaleX(0.8f);
        successBinding.getRoot().setScaleY(0.8f);

        successBinding.getRoot().animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Open download folder safely
     */
    private void openDownloadFolder() {
        try {
            // Try to open file manager to the download folder
            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                Uri folderUri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".fileprovider",
                        downloadedFile.getParentFile());
                intent.setDataAndType(folderUri, "resource/folder");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(downloadedFile.getParentFile()), "resource/folder");
            }

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
            } else {
                // Fallback: Show file path in toast
                showFilePathInfo();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to open folder", e);
            showFilePathInfo();
        }
    }

    /**
     * Install addon by opening with Minecraft
     */
    private void installAddon() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                Uri fileUri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".fileprovider", downloadedFile);
                intent.setDataAndType(fileUri, "application/octet-stream");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(downloadedFile), "application/octet-stream");
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
                successDialog.dismiss();
                Toast.makeText(activity, "Opening addon with Minecraft...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Minecraft not found. Please install Minecraft to open addon files.",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to install addon", e);
            Toast.makeText(activity, "Failed to open addon file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show file path information as fallback
     */
    private void showFilePathInfo() {
        Toast.makeText(activity, "File saved to: " + downloadedFile.getAbsolutePath(),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Ensure proper file extension
     */
    private String ensureProperExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "addon_file.mcaddon";
        }

        String lowerCase = fileName.toLowerCase();
        if (!lowerCase.endsWith(".mcaddon") &&
                !lowerCase.endsWith(".mcpack") &&
                !lowerCase.endsWith(".mcworld") &&
                !lowerCase.endsWith(".zip")) {
            return fileName + ".mcaddon";
        }

        return fileName;
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Format download speed for display
     */
    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond <= 0) return "0 B/s";

        final String[] units = new String[]{"B/s", "KB/s", "MB/s", "GB/s"};
        int digitGroups = (int) (Math.log10(bytesPerSecond) / Math.log10(1024));

        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(bytesPerSecond / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Check if download is in progress
     */
    public boolean isDownloading() {
        return isDownloading;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        isDownloading = false;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (successDialog != null && successDialog.isShowing()) {
            successDialog.dismiss();
        }
    }
}