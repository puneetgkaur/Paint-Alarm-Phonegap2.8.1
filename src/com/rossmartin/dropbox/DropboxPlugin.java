package com.rossmartin.dropbox;

import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileStatus;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxFileSystem.PathListener.Mode;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxSyncStatus;


/**
 * PhoneGap Dropbox Sync Plugin for Android - Ross Martin 8/21/13.
 */
public class DropboxPlugin extends CordovaPlugin {
    
    private static final String TAG = "DropboxPlugin";
    private static final String PLUGIN_ERROR = "DropboxPlugin Error";
    private static final String APP_KEY = "flgfzhdm7043qw6"; // Your app key here
    private static final String APP_SECRET = "6mrp47xpk2y5okf"; // Your app secret here
    static final int REQUEST_LINK_TO_DBX = 1337;  // This value is up to you, it must be the same as in your main activity though
    private DbxAccountManager mDbxAcctMgr;
    private static List<File> localFileList;
    private static int numFilesToUpload;
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "execute method starting");
        mDbxAcctMgr = DbxAccountManager.getInstance(cordova.getActivity().getApplicationContext(), APP_KEY, APP_SECRET);
        String message = args.getString(0);
        if (action.equals("checkLink")) {
            this.checkLink(message, callbackContext);
            return true;
        } else if (action.equals("link")) {
            this.link(message, callbackContext);
            return true;
        } else if (action.equals("unlink")) {
            this.unlink(message, callbackContext);
            return true;
        } else if (action.equals("listFolder")) {
            this.listFolder(message, callbackContext);
            return true;
        } else if (action.equals("addObserver")) {
            this.addObserver(message, callbackContext);
            return true;
        } else if (action.equals("readData")) {
            this.readData(message, callbackContext);
            return true;
        } else if (action.equals("readString")) {
            this.readString(message, callbackContext);
            return true;
        } else if (action.equals("uploadFile")) {
            this.uploadFile(message, callbackContext);
            return true;
        } else if (action.equals("uploadFolder")) {
            this.uploadFolder(message, callbackContext);
            return true;
        }
        return false;
    }
    
    private void checkLink(String message, CallbackContext callbackContext) {
        Log.v(TAG, "checkLink method executing");
        if (mDbxAcctMgr.hasLinkedAccount()){
            callbackContext.success();
        } else {
            callbackContext.error("User not authenticated yet");
        }
    }

    private void link(String message, CallbackContext callbackContext) {
        Log.v(TAG, "link method executing");
        mDbxAcctMgr.startLink(cordova.getActivity(), REQUEST_LINK_TO_DBX);
        callbackContext.success();
    }
    
    private void unlink(String message, CallbackContext callbackContext) {
        Log.v(TAG, "unlink method executing");
        mDbxAcctMgr.unlink();
        callbackContext.success();
    }
    
    private void listFolder(final String message, final CallbackContext callbackContext) {
        Log.v(TAG, "listFolder method executing");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                DbxFileSystem dbxFs;
                JSONArray jsonArray = new JSONArray();
                try {
                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                    List<DbxFileInfo> infos = dbxFs.listFolder(new DbxPath(message));
                    for (DbxFileInfo info : infos) {
                        JSONObject dbFile = new JSONObject();
                        dbFile.put("path", info.path);
                        dbFile.put("modifiedTime", info.modifiedTime);
                        dbFile.put("size", info.size);
                        dbFile.put("isFolder", info.isFolder);
                        jsonArray.put(dbFile);
                    }
                    callbackContext.success(jsonArray);
                } catch (Unauthorized e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (DbxException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                }
            }
        });
        
    }
    
    private void addObserver(final String message, final CallbackContext callbackContext) {
        Log.v(TAG, "addObserver method executing");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                DbxFileSystem dbxFs;
                try {
                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                    dbxFs.addPathListener(new DbxFileSystem.PathListener() {

                        @Override
                        public void onPathChange(DbxFileSystem arg0, DbxPath arg1, Mode arg2) {
                            webView.loadUrl("javascript:dropbox_fileChange();");
                        }
                        
                    }, new DbxPath(message), Mode.PATH_OR_CHILD);
                    
                    dbxFs.addSyncStatusListener(new DbxFileSystem.SyncStatusListener() {
                        
                        @Override
                        public void onSyncStatusChange(DbxFileSystem fs) {
                            try {
                                DbxSyncStatus dbSyncStatus = fs.getSyncStatus();
                                if (! dbSyncStatus.anyInProgress()) {
                                    webView.loadUrl("javascript:dropbox_onSyncStatusChange('none');");
                                } else {
                                    webView.loadUrl("javascript:dropbox_onSyncStatusChange('sync');");
                                }
                            } catch (DbxException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Unauthorized e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                }
            }
        });
    }
    
    private void readData(final String message, final CallbackContext callbackContext) {
        Log.v(TAG, "readData method executing");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                DbxFileSystem dbxFs;
                
                try {
                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                    DbxPath filePath = new DbxPath(message);
                    DbxFile file = dbxFs.open(filePath);
                    try {
                        FileInputStream contents = file.getReadStream();
                        BufferedInputStream buf = new BufferedInputStream(contents);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        // read until a single byte is available
                        while(buf.available() > 0) {
                           // read the byte and convert the integer to character
                           char c = (char)buf.read();
                           baos.write(c);
                        }
                        callbackContext.success(baos.toByteArray());
                        baos.flush();
                        buf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        callbackContext.error(PLUGIN_ERROR);
                    } finally {
                        file.close();
                    }
                    
                } catch (Unauthorized e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (DbxException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } 
            }
        });
        
        
    }
    
    private void readString(final String message, final CallbackContext callbackContext) {
        Log.v(TAG, "readString method executing");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                DbxFileSystem dbxFs;
                
                try {
                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                    DbxPath filePath = new DbxPath(message);
                    DbxFile file = dbxFs.open(filePath);
                    try {
                        String contents = file.readString();
                        callbackContext.success(contents);
                    } catch (IOException e) {
                        e.printStackTrace();
                        callbackContext.error(PLUGIN_ERROR);
                    } finally {
                        file.close();
                    }
                } catch (Unauthorized e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (DbxException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                }
            }
        });
    }
    
    private void uploadFile(final String message, final CallbackContext callbackContext) {
        Log.v(TAG, "uploadFile method executing");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                DbxFileSystem dbxFs;
                
                try {
                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                    File uploadFile = resolveLocalFileSystemURI(message);
                    DbxPath filePath = new DbxPath(DbxPath.ROOT, uploadFile.getName());
                    DbxFile dbxFile;
                    if (dbxFs.exists(filePath)){
                        dbxFile = dbxFs.open(filePath);
                    } else {
                        dbxFile = dbxFs.create(filePath);
                    }
                    dbxFile.writeFromExistingFile(uploadFile, false);
                    dbxFs.syncNowAndWait();
                    dbxFile.addListener(new DbxFile.Listener() {
                        @Override
                        public void onFileChange(DbxFile file) {
                            Log.v(TAG, "onFileChange is firing");
                            DbxFileStatus status;
                            try {
                                status = file.getSyncStatus();
                                long sizeTransferred = status.bytesTransferred;
                                Log.v(TAG, "upload, sizeTransferred -> " + sizeTransferred);
                                if (sizeTransferred == -1) { // transfer done
                                    file.close();
                                    callbackContext.success();
                                }
                            } catch (DbxException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Unauthorized e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (DbxException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (IOException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (JSONException e) {
                    callbackContext.error(PLUGIN_ERROR);
                }
            }
        });
    }
    
    private void uploadFolder(final String message, final CallbackContext callbackContext) {
        Log.v(TAG, "uploadFolder method executing");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                DbxFileSystem dbxFs;
                
                try {
                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                    File uploadPath = resolveLocalFileSystemURI(message);
                    localFileList = new ArrayList<File>();
                    recursiveDirectorySearch(uploadPath);
                    Log.v(TAG, "uploadFolder after recursiveDirectorySearch method call, localFileList -> " + localFileList);
                    numFilesToUpload = localFileList.size();
                    if (numFilesToUpload > 0) {
                        for (File file : localFileList) {
                            DbxFile dbxFile;
                            String parentName = uploadPath.getName();
                            int needle = file.getPath().indexOf(parentName);
                            String fileUploadName = file.getPath().substring(needle);
                            Log.v(TAG, "fileUploadName -> " + fileUploadName);
                            
                            if (file.isDirectory()) {
                                DbxPath filePath = new DbxPath(DbxPath.ROOT, fileUploadName);
                                if (!dbxFs.exists(filePath)) {
                                    Log.v(TAG, "Creating new directory in Dropbox, directory name -> " + fileUploadName);
                                    dbxFs.createFolder(filePath);
                                }
                                numFilesToUpload--;
                                if (numFilesToUpload == 0) {
                                    callbackContext.success();
                                }
                            } else {
                                DbxPath filePath = new DbxPath(DbxPath.ROOT, fileUploadName);
                                if (dbxFs.exists(filePath)) {
                                    dbxFile = dbxFs.open(filePath);
                                } else {
                                    dbxFile = dbxFs.create(filePath);
                                }
                                dbxFile.writeFromExistingFile(file, false);
                                dbxFs.syncNowAndWait();
                                dbxFile.addListener(new DbxFile.Listener() {
                                    @Override
                                    public void onFileChange(DbxFile file) {
                                        DbxFileStatus status;
                                        try {
                                            status = file.getSyncStatus();
                                            long sizeTransferred = status.bytesTransferred;
                                            if (sizeTransferred == -1) { // transfer done
                                                file.close();
                                                numFilesToUpload--;
                                                if (numFilesToUpload == 0) {
                                                    callbackContext.success();
                                                }
                                            }
                                        } catch (DbxException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    } else { // just an empty directory to make
                        if (uploadPath.isDirectory()) {
                            DbxPath filePath = new DbxPath(DbxPath.ROOT, uploadPath.getName());
                            if (!dbxFs.exists(filePath)) {
                                Log.v(TAG, "Creating new directory in Dropbox, directory name -> " + uploadPath.getName());
                                dbxFs.createFolder(filePath);
                                callbackContext.success();
                            }
                        } 
                    }
                } catch (Unauthorized e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (DbxException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (IOException e) {
                    e.printStackTrace();
                    callbackContext.error(PLUGIN_ERROR);
                } catch (JSONException e) {
                    callbackContext.error(PLUGIN_ERROR);
                }
            }
        });
    }
    
    @SuppressWarnings("deprecation")
    private File resolveLocalFileSystemURI(String url) throws IOException, JSONException {
        String decoded = URLDecoder.decode(url, "UTF-8");

        File fp = null;

        // Handle the special case where you get an Android content:// uri.
        if (decoded.startsWith("content:")) {
            Cursor cursor = this.cordova.getActivity().managedQuery(Uri.parse(decoded), new String[] { MediaStore.Images.Media.DATA }, null, null, null);
            // Note: MediaStore.Images/Audio/Video.Media.DATA is always "_data"
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            fp = new File(cursor.getString(column_index));
        } else {
            // Test to see if this is a valid URL first
            @SuppressWarnings("unused")
            URL testUrl = new URL(decoded);

            if (decoded.startsWith("file://")) {
                int questionMark = decoded.indexOf("?");
                if (questionMark < 0) {
                    fp = new File(decoded.substring(7, decoded.length()));
                } else {
                    fp = new File(decoded.substring(7, questionMark));
                }
            } else {
                fp = new File(decoded);
            }
        }

        if (!fp.exists()) {
            throw new FileNotFoundException();
        }
        if (!fp.canRead()) {
            throw new IOException();
        }
        return fp;
    }
    
    private static void recursiveDirectorySearch(File dir) {
        try {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    Log.v(TAG, "directory:" + file.getCanonicalPath());
                    localFileList.add(file);
                    recursiveDirectorySearch(file);
                } else {
                    Log.v(TAG, "file:" + file.getCanonicalPath());
                    localFileList.add(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   
}
