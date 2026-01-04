package com.suchi.musicisland.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.suchi.musicisland.Executor.AppExecutors;
import com.suchi.musicisland.Listener.MainUIChangeNotifier;
import com.suchi.musicisland.MusicScanner;

public class ImportMusicActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        openDirectoryPicker();
    }

    /** 打开系统文件管理器选择文件夹 */
    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        // 申请并保留 URI 访问权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    /** 用户选择文件夹后的回调 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 用户取消操作
        if (resultCode != RESULT_OK) {
            finish();
            return;
        }

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && data != null) {

            Uri treeUri = data.getData();
            if (treeUri == null) {
                Toast.makeText(this, "选择的文件夹无效", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 保留权限（重启也能访问）
            getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            AppExecutors.DB.execute(() -> {
                MusicScanner scanner = new MusicScanner(this);
                scanner.scanFolder(treeUri);

                runOnUiThread(() -> {
                    MainUIChangeNotifier.getInstance().notifyUIChanged();
                });
            });

            // 返回 MainActivity
            finish();
        }
    }
}
