package app3cm.mg2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.i18next.android.I18Next;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CommonUtil {

    public static final String TAG = "MG2";

    public static String getAssetJsonData(Context context, String jsonPath) throws IOException {
        String json;

        InputStream is = context.getAssets().open(jsonPath);
        int size = is.available();
        byte[] buffer = new byte[size];
        while (true) {
            int count = is.read(buffer);
            if (count == -1) {
                break;
            }
        }
        is.close();
        json = new String(buffer, StandardCharsets.UTF_8);

        return json;
    }

    public static void promptLeaveAppDialog(Context context, I18Next i18Next) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(i18Next.t("app.leave_dialog_title"))
                .setMessage(i18Next.t("app.leave_dialog_message"))
                .setPositiveButton(
                        i18Next.t("app.leave_dialog_button_confirm"),
                        (DialogInterface dialog, int index) -> {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("SHOULD_FINISH_APP", true);
                            context.startActivity(intent);
                        })
                .setNegativeButton(
                        i18Next.t("app.leave_dialog_button_decline"), null);
        builder.create().show();
    }

}
