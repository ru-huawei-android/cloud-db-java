package com.huawei.agc.clouddb.java.util;

import android.util.Log;

import com.huawei.agc.clouddb.java.model.Book;
import com.huawei.agconnect.cloud.database.AGConnectCloudDB;
import com.huawei.agconnect.cloud.database.CloudDBZone;
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig;
import com.huawei.agconnect.cloud.database.CloudDBZoneObjectList;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.agconnect.cloud.database.CloudDBZoneSnapshot;
import com.huawei.agconnect.cloud.database.CloudDBZoneTask;
import com.huawei.agconnect.cloud.database.ListenerHandler;
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.huawei.agc.clouddb.java.model.ObjectTypeInfoHelper.getObjectTypeInfo;
import static com.huawei.agc.clouddb.java.util.Constants.CLOUD_TAG;
import static com.huawei.agc.clouddb.java.util.Constants.ZONE_NAME;

public class CloudDB {

    private static volatile CloudDB cloudDB;

    private AGConnectCloudDB agConnectCloudDB  = AGConnectCloudDB.getInstance();
    private CloudDBZone cloudDBZone;
    private ListenerHandler register;
    private CloudDBZoneConfig config;
    private UiCallBack uiCallBack;
    private int bookIndex = 0;
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private CloudDB() {
    }

    public static CloudDB getInstance() {
        CloudDB localCloudDB = cloudDB;
        if (localCloudDB == null) {
            synchronized (CloudDB.class) {
                localCloudDB = cloudDB;
                if (localCloudDB == null) {
                    cloudDB = localCloudDB = new CloudDB();
                }
            }
        }
        return localCloudDB;
    }

    /**
     * Call AGConnectCloudDB.createObjectType to init schema
     */
    public void createObjectType() {
        try {
            agConnectCloudDB.createObjectType(getObjectTypeInfo());
        } catch (AGConnectCloudDBException e) {
            Log.w(CLOUD_TAG, "createObjectType: " + e.getMessage());
        }
    }

    /**
     * Call AGConnectCloudDB.openCloudDBZone to open a cloudDBZone.
     * We set it with cloud cache mode, and data can be store in local storage
     */
    public void openCloudDBZone() {
        config = new CloudDBZoneConfig(
                ZONE_NAME,
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC
        );

        config.setPersistenceEnabled(true);
        try {
            cloudDBZone = agConnectCloudDB.openCloudDBZone(config, true);
        } catch (AGConnectCloudDBException e) {
            Log.w(CLOUD_TAG, "openCloudDBZone: " + e.getMessage());
        }
    }


    /**
     * Call AGConnectCloudDB.closeCloudDBZone
     */
    public void closeCloudDBZone() {
        try {
            register.remove();
            agConnectCloudDB.closeCloudDBZone(cloudDBZone);
        } catch (AGConnectCloudDBException e) {
            Log.w(CLOUD_TAG, "closeCloudDBZone: " + e.getMessage());
        }
    }

    /**
     * Query all books in storage from cloud side with CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
     */
    public void getAll() {
        if (cloudDBZone == null) {
            Log.w(CLOUD_TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        CloudDBZoneTask<CloudDBZoneSnapshot<Book>> queryTask = cloudDBZone.executeQuery(
                CloudDBZoneQuery.where(Book.class),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);

        queryTask.addOnSuccessListener(snapshot -> {
            processQueryResult(snapshot);
        }).addOnFailureListener(e -> {
            Log.e(CLOUD_TAG, Objects.requireNonNull(e.getMessage()));
        });
    }

    public void processQueryResult(CloudDBZoneSnapshot<Book> snapshot) {
        CloudDBZoneObjectList<Book> bookInfoCursor = snapshot.getSnapshotObjects();
        List<Book> bookList = new ArrayList();
        try {
            while (bookInfoCursor.hasNext()) {
                Book book = bookInfoCursor.next();
                bookList.add(book);
                updateBookIndex(book);
            }
        } catch (AGConnectCloudDBException e) {
            Log.w(CLOUD_TAG, "processQueryResult: " + e.getMessage());
        }
        snapshot.release();
        uiCallBack.onStart(bookList);
    }

    /**
     * Delete book
     *
     * @param bookList books selected by user
     */
    public void deleteBook(List<Book> bookList) {
        if (cloudDBZone == null) {
            Log.w(CLOUD_TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        CloudDBZoneTask<Integer> deleteTask = cloudDBZone.executeDelete(bookList);
        deleteTask.addOnSuccessListener(integer -> {
            Log.d(CLOUD_TAG, "Item deleted");
        }).addOnFailureListener(e -> {
            Log.e(CLOUD_TAG, "Delete book is failed: " + e.getMessage());
        });
    }

    /**
     * Insert book
     *
     * @param book book added or modified from local
     */
    public void insertBook(Book book) {
        if (cloudDBZone == null) {
            Log.w(CLOUD_TAG, "CloudDBZone is null, try re-open it");
            return;
        }

        CloudDBZoneTask<Integer> insertTask  = cloudDBZone.executeUpsert(book);
        insertTask.addOnSuccessListener(integer -> {
            Log.d(CLOUD_TAG, "inserted");
            uiCallBack.onAddItem(book);
        }).addOnFailureListener(e -> {
            Log.e(CLOUD_TAG, "onFailure: " + e.getMessage());
        });
    }

    /**
     * Get max id of books
     *
     * @return max book id
     */
    public int getBookIndex() {
        try {
            readWriteLock.readLock().lock();
            return bookIndex;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void updateBookIndex(Book book) {
        try {
            readWriteLock.writeLock().lock();
            if (book.getId() != null && bookIndex < book.getId()) {
                bookIndex = book.getId();
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Add a callback to update book info list
     *
     * @param uiCallBack callback to update book list
     */
    public void addCallBacks(UiCallBack uiCallBack) {
        this.uiCallBack = uiCallBack;
    }

    /**
     * Call back to update ui
     */
    public interface UiCallBack {
        void onStart(List<Book> books);
        void onAddItem(Book book);
    }
}
