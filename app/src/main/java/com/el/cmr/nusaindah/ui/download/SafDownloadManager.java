package com.el.cmr.nusaindah.ui.download;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.el.cmr.nusaindah.databinding.DialogDownloadProgressBinding;
import com.el.cmr.nusaindah.databinding.DialogDownloadSuccessBinding;
import com.el.cmr.nusaindah.databinding.DialogStoragePermissionBinding;
import com.el.cmr.nusaindah.ui.storage.StorageAccessManager;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.text.DecimalFormat;

/**
 * SAF-compliant Download Manager
 * Uses Storage Access Framework for file saving
 */
public class SafDownloadManager implements StorageAccessManager.StorageAccessListener {

    private static final String TAG = "SafDownloadManager";

    private AppCompatActivity activity;
    private FirebaseStorage storage;
    private StorageAccessManager storageManager;

    // Dialogs
    private Dialog progressDialog;
    private Dialog successDialog;
    private Dialog storagePermissionDialog;

    // UI Bindings
    private DialogDownloadProgressBinding progressBinding;
    private DialogDownloadSuccessBinding successBinding;
    private DialogStoragePermissionBinding storageBinding;

    // Download state
    private boolean isDownloading = false;
    private String fileName;
    private Uri savedFileUri;
    private DownloadCallback currentCallback;
    private String currentFirebasePath;

    // Download statistics
    private long totalBytes = 0;
    private long downloadedBytes = 0;
    private long startTime;

    public static final String D_FOLDER_NAME = "Minecraft Addons";

    public interface DownloadCallback {
        void onDownloadStart();
        void onProgress(int progress, long downloaded, long total, String speed);
        void onSuccess(String filePath);
        void onError(String error);
        void onCancelled();
    }

    public SafDownloadManager(AppCompatActivity activity) {
        this.activity = activity;
        this.storage = FirebaseStorage.getInstance();
        this.storageManager = new StorageAccessManager(activity, this);
    }

    /**
     * Start download with SAF compliance
     */
    public void startDownload(String firebaseStoragePath, String fileName, DownloadCallback callback) {
        if (isDownloading) {
            Toast.makeText(activity, "Download already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        this.fileName = ensureProperExtension(fileName);
        this.currentCallback = callback;
        this.currentFirebasePath = firebaseStoragePath;

        // Check if folder is selected
        if (!storageManager.isFolderSelected()) {
            showStoragePermissionDialog();
            return;
        }

        // Start download process
        startFirebaseDownload(firebaseStoragePath);
    }

    /**
     * Show storage permission dialog
     */
    private void showStoragePermissionDialog() {
        storageBinding = DialogStoragePermissionBinding.inflate(LayoutInflater.from(activity));

        storagePermissionDialog = new Dialog(activity);
        storagePermissionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        storagePermissionDialog.setContentView(storageBinding.getRoot());
        storagePermissionDialog.setCancelable(true);
        storagePermissionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set dialog size
        Window window = storagePermissionDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Set folder info if available
        storageBinding.tvCurrentFolder.setText(storageManager.getSelectedFolderInfo());

        // Setup button listeners
        storageBinding.btnSelectFolder.setOnClickListener(v -> {
            storagePermissionDialog.dismiss();
            storageManager.requestFolderAccess();
        });

        storageBinding.btnCancel.setOnClickListener(v -> {
            storagePermissionDialog.dismiss();
            if (currentCallback != null) {
                currentCallback.onCancelled();
            }
        });

        storagePermissionDialog.show();

        // Entrance animation
        storageBinding.getRoot().setAlpha(0f);
        storageBinding.getRoot().setScaleX(0.8f);
        storageBinding.getRoot().setScaleY(0.8f);

        storageBinding.getRoot().animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Start Firebase download process
     */
    private void startFirebaseDownload(String firebaseStoragePath) {
        Log.d(TAG, "Starting download: " + firebaseStoragePath);

        StorageReference storageRef = storage.getReference().child(firebaseStoragePath);

        // Show progress dialog
        showProgressDialog();

        // Get file metadata
        storageRef.getMetadata().addOnSuccessListener(metadata -> {
            totalBytes = metadata.getSizeBytes();

            // Use real Firebase filename if available
            String realFirebaseFileName = metadata.getName();
            if (realFirebaseFileName != null && !realFirebaseFileName.isEmpty()) {
                this.fileName = realFirebaseFileName;
                Log.d(TAG, "Real Firebase file name: " + this.fileName);

                if (progressBinding != null) {
                    progressBinding.tvFileName.setText(this.fileName);
                }
            }

            updateProgress(0, "Preparing download...");

            startTime = System.currentTimeMillis();
            isDownloading = true;
            if (currentCallback != null) {
                currentCallback.onDownloadStart();
            }

            // Start actual download
            storageRef.getStream().addOnSuccessListener(taskSnapshot -> {
                new Thread(() -> processDownloadWithSAF(taskSnapshot.getStream())).start();

            }).addOnFailureListener(exception -> {
                isDownloading = false;
                hideProgressDialog();
                Log.e(TAG, "Download stream failed", exception);
                if (currentCallback != null) {
                    currentCallback.onError("Download failed: " + exception.getMessage());
                }
            });

        }).addOnFailureListener(exception -> {
            isDownloading = false;
            hideProgressDialog();
            Log.e(TAG, "Metadata fetch failed", exception);
            if (currentCallback != null) {
                currentCallback.onError("Failed to get file info: " + exception.getMessage());
            }
        });
    }

    /**
     * Process download using SAF
     */
    private void processDownloadWithSAF(InputStream inputStream) {
        try {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            // Save file using SAF
            boolean success = storageManager.saveFileToSelectedFolder(inputStream, fileName);

            if (success && isDownloading) {
                mainHandler.post(() -> {
                    isDownloading = false;
                    hideProgressDialog();
                    showSuccessDialog();
                    if (currentCallback != null) {
                        currentCallback.onSuccess("File saved successfully");
                    }
                });
            } else if (!isDownloading) {
                // Download cancelled
                mainHandler.post(() -> {
                    if (currentCallback != null) {
                        currentCallback.onCancelled();
                    }
                });
            }

            inputStream.close();

        } catch (Exception e) {
            isDownloading = false;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                hideProgressDialog();
                if (currentCallback != null) {
                    currentCallback.onError("Download failed: " + e.getMessage());
                }
            });
            Log.e(TAG, "Download processing failed", e);
        }
    }

    /**
     * Show progress dialog with animations
     */
    private void showProgressDialog() {
        progressBinding = DialogDownloadProgressBinding.inflate(LayoutInflater.from(activity));

        progressDialog = new Dialog(activity);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(progressBinding.getRoot());
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set dialog size
        Window window = progressDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9);
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

        // Update text
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
     * Cancel download
     */
    private void cancelDownload() {
        isDownloading = false;

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
     * Hide progress dialog
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
     * Show success dialog
     */
    /**
     * Show success dialog dengan tombol How to Use
     */
    private void showSuccessDialog() {
        successBinding = DialogDownloadSuccessBinding.inflate(LayoutInflater.from(activity));

        successDialog = new Dialog(activity);
        successDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        successDialog.setContentView(successBinding.getRoot());
        successDialog.setCancelable(true);
        successDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set dialog size
        Window window = successDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Set file information
        successBinding.tvSuccessFileName.setText(this.fileName);
        successBinding.tvFilePath.setText(storageManager.getSelectedFolderInfo() + "/" + D_FOLDER_NAME + "/" + fileName);

        // Setup button listeners
        successBinding.btnOpenFolder.setOnClickListener(v -> openDownloadFolder());
        successBinding.btnHowToUse.setOnClickListener(v -> showHowToUseDialog());  // UBAH INI
        successBinding.btnCloseSuccess.setOnClickListener(v -> {
            successBinding.getRoot().animate()
                    .alpha(0f)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(200)
                    .withEndAction(() -> successDialog.dismiss())
                    .start();
        });

        successDialog.show();

        // Entrance animation
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
     * Open download folder
     */
    private void openDownloadFolder() {
        // Step 1: Show folder information first (always safe)
        String folderInfo = storageManager.getSelectedFolderInfo() + "/" + D_FOLDER_NAME;
        Toast.makeText(activity, "File saved to: " + folderInfo, Toast.LENGTH_LONG).show();

        // Step 2: Try to open file manager (safe fallback)
        try {
            // Open generic file manager app
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                Intent chooser = Intent.createChooser(intent, "Open File Manager");
                activity.startActivity(chooser);
            } else {
                // Final fallback: Show detailed info dialog
                showDetailedFolderInfo();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to open file manager", e);
            showDetailedFolderInfo();
        }
    }

    /**
     * Show detailed folder information with copy option
     */
    private void showDetailedFolderInfo() {
        String folderPath = storageManager.getSelectedFolderInfo();

        String message = "📁 Your file has been saved to:\n\n" +
                "📂 Folder: " + folderPath + "\n" +
                "📂 Subfolder: " + D_FOLDER_NAME + "\n" +
                "📄 File: " + fileName + "\n\n" +
                "💡 Open your file manager and navigate to this location to find your downloaded addon.";

        new android.app.AlertDialog.Builder(activity)
                .setTitle("📥 Download Complete")
                .setMessage(message)
                .setPositiveButton("📋 Copy Path", (dialog, which) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Folder Path",
                            folderPath + "/" + D_FOLDER_NAME);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(activity, "📋 Path copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("📱 Open Files App", (dialog, which) -> {
                    try {
                        Intent filesIntent = activity.getPackageManager().getLaunchIntentForPackage("com.google.android.documentsui");
                        if (filesIntent != null) {
                            activity.startActivity(filesIntent);
                        } else {
                            Toast.makeText(activity, "Please open your file manager manually", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(activity, "Please open your file manager manually", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("✅ OK", null)
                .setCancelable(true)
                .show();
    }

    // StorageAccessManager.StorageAccessListener implementations
    @Override
    public void onFolderSelected(Uri folderUri, String folderPath) {
        Toast.makeText(activity, "Folder selected: " + folderPath, Toast.LENGTH_SHORT).show();

        // Restart download if we were in the middle of one
        if (currentCallback != null && currentFirebasePath != null) {
            startFirebaseDownload(currentFirebasePath);
        }
    }

    @Override
    public void onFileSaved(Uri fileUri, String fileName) {
        this.savedFileUri = fileUri;
        Log.d(TAG, "File saved successfully: " + fileName);
    }

    @Override
    public void onStorageError(String error) {
        Log.e(TAG, "Storage error: " + error);
        if (currentCallback != null) {
            currentCallback.onError(error);
        }
    }

    @Override
    public void onPermissionDenied() {
        if (currentCallback != null) {
            currentCallback.onCancelled();
        }
    }

    /**
     * Utility methods
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

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void cleanup() {
        isDownloading = false;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (successDialog != null && successDialog.isShowing()) {
            successDialog.dismiss();
        }

        if (storagePermissionDialog != null && storagePermissionDialog.isShowing()) {
            storagePermissionDialog.dismiss();
        }
    }

    /**
     * Show comprehensive How to Use dialog for all Minecraft file formats
     */
    private void showHowToUseDialog() {
        // Detect file type
        String fileType = detectFileType(fileName);
        String instructions = generateInstructionsForFileType(fileType, fileName);

        new android.app.AlertDialog.Builder(activity)
                .setTitle("🎮 How to Install " + fileType)
                .setMessage(instructions)
                .setPositiveButton("📺 Watch Tutorial", (dialog, which) -> {
                    openYouTubeTutorial();
                })
                .setNeutralButton("📋 Copy Instructions", (dialog, which) -> {
                    copyInstructionsToClipboard(instructions);
                })
                .setNegativeButton("✅ Got It", null)
                .setCancelable(true)
                .show();
    }

    /**
     * Detect file type from filename
     */
    private String detectFileType(String fileName) {
        String lowerCase = fileName.toLowerCase();
        if (lowerCase.endsWith(".mcaddon")) {
            return "Minecraft Addon (.mcaddon)";
        } else if (lowerCase.endsWith(".mcpack")) {
            return "Minecraft Pack (.mcpack)";
        } else if (lowerCase.endsWith(".mcworld")) {
            return "Minecraft World (.mcworld)";
        } else if (lowerCase.endsWith(".zip")) {
            return "Minecraft Package (.zip)";
        } else {
            return "Minecraft Content";
        }
    }

    /**
     * Generate specific instructions based on file type
     */
    private String generateInstructionsForFileType(String fileType, String fileName) {
        String basePath = "📂 " + storageManager.getSelectedFolderInfo() + "/" + D_FOLDER_NAME;
        String commonSteps = "1️⃣ Open your File Manager\n" +
                "2️⃣ Navigate to: " + basePath + "\n" +
                "3️⃣ Find your file: 📄 " + fileName + "\n\n";

        if (fileType.contains(".mcaddon")) {
            return generateMcAddonInstructions(commonSteps, fileName);
        } else if (fileType.contains(".mcpack")) {
            return generateMcPackInstructions(commonSteps, fileName);
        } else if (fileType.contains(".mcworld")) {
            return generateMcWorldInstructions(commonSteps, fileName);
        } else if (fileType.contains(".zip")) {
            return generateZipInstructions(commonSteps, fileName);
        } else {
            return generateGenericInstructions(commonSteps, fileName);
        }
    }

    /**
     * Instructions for .mcaddon files
     */
    private String generateMcAddonInstructions(String commonSteps, String fileName) {
        return "🎮 How to Install Minecraft Addon (.mcaddon):\n\n" +
                commonSteps +
                "📱 INSTALLATION:\n" +
                "4️⃣ Tap the .mcaddon file\n" +
                "5️⃣ Choose \"Open with Minecraft\"\n" +
                "6️⃣ Minecraft will automatically import the addon\n" +
                "7️⃣ Wait for \"Import Successful\" message\n\n" +
                "🎯 WHERE TO FIND IT:\n" +
                "• Open Minecraft → Settings\n" +
                "• Go to \"Global Resources\"\n" +
                "• Find your addon in \"My Packs\" section\n" +
                "• Activate it by clicking the \"+\" button\n\n" +
                "🌍 FOR NEW WORLDS:\n" +
                "• Create New World → Behavior Packs\n" +
                "• Find and activate your addon\n\n" +
                "💡 Note: Addons include both Resource Packs and Behavior Packs";
    }

    /**
     * Instructions for .mcpack files
     */
    private String generateMcPackInstructions(String commonSteps, String fileName) {
        return "📦 How to Install Minecraft Pack (.mcpack):\n\n" +
                commonSteps +
                "📱 INSTALLATION:\n" +
                "4️⃣ Tap the .mcpack file\n" +
                "5️⃣ Choose \"Open with Minecraft\"\n" +
                "6️⃣ Minecraft will import the pack automatically\n" +
                "7️⃣ Wait for \"Import Successful\" message\n\n" +
                "🎯 WHERE TO FIND IT:\n" +
                "• Open Minecraft → Settings\n" +
                "• Check both sections:\n" +
                "  📁 \"Global Resources\" (for Resource Packs)\n" +
                "  📁 \"Behavior Packs\" (for Behavior Packs)\n" +
                "• Find your pack in \"My Packs\"\n" +
                "• Activate by clicking the \"+\" button\n\n" +
                "🌍 USING IN WORLDS:\n" +
                "• Create/Edit World → Resource Packs or Behavior Packs\n" +
                "• Select and activate your imported pack\n\n" +
                "💡 Note: .mcpack can be either Resource Pack or Behavior Pack";
    }

    /**
     * Instructions for .mcworld files
     */
    private String generateMcWorldInstructions(String commonSteps, String fileName) {
        return "🌍 How to Install Minecraft World (.mcworld):\n\n" +
                commonSteps +
                "📱 INSTALLATION:\n" +
                "4️⃣ Tap the .mcworld file\n" +
                "5️⃣ Choose \"Open with Minecraft\"\n" +
                "6️⃣ Minecraft will import the world automatically\n" +
                "7️⃣ Wait for \"Import Successful\" message\n\n" +
                "🎯 WHERE TO FIND IT:\n" +
                "• Open Minecraft\n" +
                "• Go to \"Play\" section\n" +
                "• Look for your world in the worlds list\n" +
                "• The world name will appear as imported\n\n" +
                "🚀 HOW TO PLAY:\n" +
                "• Simply tap the world to start playing\n" +
                "• All included addons/packs are already activated\n" +
                "• No additional setup required!\n\n" +
                "💡 Note: .mcworld files often include pre-installed addons and resource packs";
    }

    /**
     * Instructions for .zip files (mixed content)
     */
    private String generateZipInstructions(String commonSteps, String fileName) {
        return "📦 How to Install Minecraft Package (.zip):\n\n" +
                commonSteps +
                "📱 EXTRACTION REQUIRED:\n" +
                "4️⃣ Tap the .zip file\n" +
                "5️⃣ Choose \"Extract\" or \"Unzip\"\n" +
                "6️⃣ Extract to the same folder\n" +
                "7️⃣ You'll find multiple files inside:\n\n" +
                "📄 FILE TYPES YOU MAY FIND:\n" +
                "• 🎮 .mcaddon files → Addons (Resource + Behavior)\n" +
                "• 📦 .mcpack files → Individual packs\n" +
                "• 🌍 .mcworld files → Complete worlds\n" +
                "• 📁 Folders → Manual installation needed\n\n" +
                "🔧 INSTALLATION STEPS:\n" +
                "8️⃣ For .mcaddon, .mcpack, .mcworld files:\n" +
                "   → Tap each file → Open with Minecraft\n\n" +
                "9️⃣ For folders (behavior_packs/resource_packs):\n" +
                "   → Copy to Minecraft folders manually:\n" +
                "   📁 Android/data/com.mojang.minecraftpe/files/games/com.mojang/\n" +
                "   📁 behavior_packs/ (for behavior packs)\n" +
                "   📁 resource_packs/ (for resource packs)\n\n" +
                "💡 Tip: Start with .mcaddon and .mcpack files first - they're easier!";
    }

    /**
     * Generic instructions for unknown file types
     */
    private String generateGenericInstructions(String commonSteps, String fileName) {
        return "🎮 How to Install Minecraft Content:\n\n" +
                commonSteps +
                "📱 INSTALLATION METHODS:\n\n" +
                "METHOD 1 - Direct Install:\n" +
                "4️⃣ Tap the file\n" +
                "5️⃣ Choose \"Open with Minecraft\"\n" +
                "6️⃣ Let Minecraft handle the import\n\n" +
                "METHOD 2 - Manual Check:\n" +
                "4️⃣ Check file extension:\n" +
                "   • .mcaddon → Full addon package\n" +
                "   • .mcpack → Resource or behavior pack\n" +
                "   • .mcworld → Complete world\n" +
                "   • .zip → Extract first, then install contents\n\n" +
                "🎯 AFTER INSTALLATION:\n" +
                "• Check Minecraft → Settings → Global Resources\n" +
                "• Check Minecraft → Play (for worlds)\n" +
                "• Look in \"My Packs\" sections\n\n" +
                "💡 If unsure, try opening with Minecraft first!";
    }

    /**
     * Enhanced tutorial opening with file-type specific guidance
     */
    private void openYouTubeTutorial() {
        try {
            String tutorialUrl = activity.getResources().getString(com.el.cmr.nusaindah.R.string.url_apply_map);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tutorialUrl));
            activity.startActivity(browserIntent);

            // Show additional tip based on file type
            String fileType = detectFileType(fileName);
            String tip = "💡 Look for \"" + fileType + "\" tutorial in the video!";
            Toast.makeText(activity, tip, Toast.LENGTH_LONG).show();

            // Close success dialog after opening tutorial
            if (successDialog != null && successDialog.isShowing()) {
                successDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open tutorial", e);
            Toast.makeText(activity, "Failed to open tutorial video", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Enhanced clipboard copy with file type info
     */
    private void copyInstructionsToClipboard(String instructions) {
        try {
            String fileType = detectFileType(fileName);
            String clipboardContent = "=== " + fileType + " Installation Guide ===\n\n" +
                    instructions + "\n\n" +
                    "Generated by Nusa Indah App\n" +
                    "File: " + fileName;

            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Minecraft Installation Guide", clipboardContent);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(activity, "📋 " + fileType + " guide copied to clipboard!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy to clipboard", e);
            Toast.makeText(activity, "Failed to copy instructions", Toast.LENGTH_SHORT).show();
        }
    }
}