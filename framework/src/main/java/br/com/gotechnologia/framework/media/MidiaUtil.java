package br.com.gotechnologia.framework.media;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;

import br.com.gotechnologia.framework.listener.DownloadReferenceListener;
import br.com.gotechnologia.framework.listener.ShowFileClickListener;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Criado por Weslley Barbosa em 07/07/2017.
 */

public class MidiaUtil {


    public static File getAppDirectory(String dir) {
        return Environment.getExternalStoragePublicDirectory(dir);
    }




    /**
     * ?Baixar Arquivo pelo download manager
     */

    public static long downloadArquivo(Context context, String title, String arquivoName, Uri url, DownloadReferenceListener receiver) {

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        context.registerReceiver(getReceiver(receiver), filter);

        long downloadReference = 0;

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(url);


        request.setTitle(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }


        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, arquivoName);

        downloadReference = downloadManager.enqueue(request);

        return downloadReference;
    }

    /**
     * ?Baixar Arquivo pelo download manager
     */

    public static long downloadArquivo(Context context, String title, String arquivoName, String url, DownloadReferenceListener receiver) {

        return downloadArquivo(context,title,arquivoName,Uri.parse(url),receiver);
    }


    public static void showFileAction(Context context, Uri file) {
        Intent in = new Intent(Intent.ACTION_VIEW);

        in.setData(file);

        in.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(in);
    }

    public static void showFileAction(Context context, File file) {
        showFileAction(context, FileProvider.getUriForFile(context.getApplicationContext(), context.getApplicationContext().getPackageName() + ".provider", file));
    }

    public static void showFileAction(Context context, String file) {
        Log.i("MidiaUtil", "showFileAction: " + file);
        showFileAction(context, new File(Uri.parse(file).getPath()));
    }

    private static BroadcastReceiver getReceiver(final DownloadReferenceListener listener) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //check if the broadcast message is for our enqueued download
                long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                listener.onDownloadAction(referenceId);
            }
        };
    }

    public static void handleFileDownloaded(Long downloadReference, Snackbar bar) {
        if (bar == null)
            return;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadReference);

        DownloadManager downloadManager = (DownloadManager) bar.getContext().getSystemService(DOWNLOAD_SERVICE);

        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            int columnindex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(columnindex);

            int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
            int reason = cursor.getInt(columnReason);

            int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            String filename = cursor.getString(filenameIndex);
            switch (status) {
                case DownloadManager.STATUS_FAILED:
                    switch (reason) {
                        case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                            bar.setText("Arquivo já baixado");
                            bar.setAction("Abrir", new ShowFileClickListener(filename));
                            break;
                        case DownloadManager.ERROR_CANNOT_RESUME:
                            bar.setText("Download não pode ser recuperado");
                            break;
                        case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                            bar.setText("Dispositivo não encontrado");
                            break;

                        case DownloadManager.ERROR_FILE_ERROR:
                            bar.setText("Arquivo com erro");
                            break;
                        case DownloadManager.ERROR_HTTP_DATA_ERROR:
                            bar.setText("Arquivo inválido");
                            break;
                        case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                            bar.setText("Espaço em disco insulficiente");
                            break;
                        case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                            bar.setText("Erro de direcionamento");
                            break;
                        case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                            bar.setText("Erro de manuseio de requisição");
                            break;

                        default:
                            bar.setText("Erro desconhecido");
                            break;

                    }

                    break;
                case DownloadManager.STATUS_PAUSED:
                    String msg = "Download em pausa";
                    switch (reason) {
                        case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                            msg = msg.concat(" esperando wifi");
                            bar.setText(msg);
                            break;
                        case DownloadManager.PAUSED_UNKNOWN:
                            msg = msg.concat(" desconhecido");
                            bar.setText(msg);
                            break;
                        case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                            msg = msg.concat(" aguardando rede");
                            bar.setText(msg);
                            break;
                        case DownloadManager.PAUSED_WAITING_TO_RETRY:
                            msg = msg.concat(" aguardando nova tentativa");
                            bar.setText(msg);
                            break;
                    }
                case DownloadManager.STATUS_PENDING:
                    bar.setText("Arquivo pendente");
                    break;
                case DownloadManager.STATUS_RUNNING:
                    bar.setText("Baixando arquivo");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    bar.setText("Arquivo baixado com sucesso!");

                    bar.setAction("Abrir", new ShowFileClickListener(filename));
                    break;
            }
        }

        if (bar != null && !bar.isShown())
            bar.show();

    }


    public static  String getSelectedData(Uri data,Context context) {

        String path = null;
        String[] projection = {MediaStore.Files.FileColumns.DATA};

        Cursor cursor = context.getContentResolver().query(data, projection, null, null, null);

        if (cursor == null) {
            path = data.getPath();
        } else {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }


        return ((path == null || path.isEmpty()) ? (data.getPath()) : path);
    }

    public static String getDataString(Uri uri,Context context) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {

                final String docId;
                docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }

            // TODO handle non-primary volumes
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static  boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    public static  String getDataColumn(Context context, Uri uri,
                                String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

}
