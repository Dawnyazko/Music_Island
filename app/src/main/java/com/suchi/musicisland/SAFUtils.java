//package com.example.musicisland;
//
//import android.content.Context;
//import android.net.Uri;
//import android.os.Environment;
//import android.provider.DocumentsContract;
//
//public class SAFUtils {
//    public static String getFullPathFromTreeUri(Uri treeUri, Context context) {
//        if (treeUri == null) return null;
//
//        String docId = DocumentsContract.getTreeDocumentId(treeUri);
//        String[] split = docId.split(";");
//        String type = split[0];
//        String relativePath = "";
//        if (split.length > 1) {
//            relativePath = split[1];
//        }
//
//        if ("primary".equalsIgnoreCase(type)) {
//            return Environment.getExternalStorageDirectory() + "/" + relativePath;
//        }
//
//        return "/storage" + type + "/" +relativePath;
//    }
//}
