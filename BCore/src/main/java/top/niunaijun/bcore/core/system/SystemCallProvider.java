package top.niunaijun.bcore.core.system;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.niunaijun.bcore.utils.Slog;
import top.niunaijun.bcore.utils.compat.BundleCompat;

public class SystemCallProvider extends ContentProvider {
    public static final String TAG = "SystemCallProvider";

    @Override
    public boolean onCreate() {
        return initSystem();
    }

    private boolean initSystem() {
        BlackBoxSystem.getSystem().startup();
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Slog.d(TAG, "call: " + method + ", " + extras);
        if ("VM".equals(method)) {
            Bundle bundle = new Bundle();
            if (extras != null) {
                String name = extras.getString("_B_|_server_name_");
                BundleCompat.putBinder(bundle, "_B_|_server_", ServiceManager.getService(name));
            }
            return bundle;
        }
        return super.call(method, arg, extras);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
