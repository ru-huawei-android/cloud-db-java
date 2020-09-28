package com.huawei.agc.clouddb.java.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.agc.clouddb.java.R;
import com.huawei.agc.clouddb.java.model.Book;
import com.huawei.agc.clouddb.java.util.CloudDB;
import com.huawei.agc.clouddb.java.view.ItemAdapter;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.HwIdAuthProvider;
import com.huawei.agconnect.auth.SignInResult;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.api.entity.hwid.HwIDConstant;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;

import java.util.ArrayList;
import java.util.List;

import static com.huawei.agc.clouddb.java.util.Constants.HUAWEI_ID_SIGN_IN;
import static com.huawei.agc.clouddb.java.util.Constants.MAIN_ACTIVITY_TAG;

public class MainActivity extends AppCompatActivity implements CloudDB.UiCallBack {

    private ItemAdapter itemAdapter;
    private List<Book> items;

    //menu buttons
    private MenuItem logOutMenuItem;
    private MenuItem logInMenuItem;
    private MenuItem addMenuItem;

    private RecyclerView itemList;
    private SwipeRefreshLayout swipeRefreshLayout;

    //db
    private CloudDB cloudDB = CloudDB.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        itemList = findViewById(R.id.itemList);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        itemList.setHasFixedSize(true);
        itemList.setLayoutManager(new LinearLayoutManager(this));
        items = new ArrayList<>();

        //Refresh data from CloudDB by "swipeRefreshLayout"
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            cloudDB.getAll();
        });
    }

    /**
     * 1. Obtain accessToken in order to logIn, you must to be authorized to use all functions "CRUD".
     *
     * 2. Initialization CloudDB, have done by globally init i.e. [CloudDBQuickStartApplication].
     *
     * 4. Create Object Type. (Can be imported from the console [ObjectTypeInfoHelper])
     *    You must implement zone/dataType on the console and download java files.
     *
     * 5. Open CloudDB Zone. (This will be closed [onDestroy])
     *
     * 6. Fetch all data from the CloudDB and push it to the recyclerView.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Obtain accessToken in order to logIn and logIn
        if (requestCode == HUAWEI_ID_SIGN_IN) {
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                String accessToken = huaweiAccount.getAccessToken();
                Log.i(MAIN_ACTIVITY_TAG, "accessToken: " + accessToken);
                AGConnectAuthCredential credential = HwIdAuthProvider.credentialWithToken(accessToken);
                AGConnectAuth.getInstance().signIn(credential).addOnSuccessListener(signInResult -> {
                    AGConnectUser user = signInResult.getUser();
                    Toast.makeText(this, "Hello, " + user.getDisplayName(), Toast.LENGTH_LONG)
                        .show();

                    cloudDB.addCallBacks(this);
                    // Get AGConnectCloudDB ObjectTypeInfo
                    cloudDB.createObjectType();
                    //Create the Cloud DB zone, And open CloudDB
                    cloudDB.openCloudDBZone();
                    //fetchDataFromDb()
                    cloudDB.getAll();
                }).addOnFailureListener(e -> {
                    Log.e(MAIN_ACTIVITY_TAG, "onFailure: " + e.getMessage());
                    Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
                });
            }
        } else {
            Toast.makeText(this, "HwID signIn failed", Toast.LENGTH_LONG).show();
            Log.e(MAIN_ACTIVITY_TAG, "HwID signIn failed");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        logOutMenuItem = menu.findItem(R.id.menu_logout_button);
        logInMenuItem = menu.findItem(R.id.menu_login_button);
        addMenuItem = menu.findItem(R.id.menu_add_button);
        logIn();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_login_button:
                logIn();
                return true;
            case R.id.menu_logout_button:
                logOut();
                return true;
            case R.id.menu_add_button:
                addItem();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Add an new Item
     */
    private void addItem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.popup, null);

        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button saveButton = view.findViewById(R.id.saveButton);
        EditText title = view.findViewById(R.id.titleBook);
        EditText description = view.findViewById(R.id.descriptionBook);
        TextView titlePage = view.findViewById(R.id.titlePage);

        titlePage.setText(R.string.enter_book);
        saveButton.setText(R.string.save);

        saveButton.setOnClickListener( v -> {
            Book item = new Book();
            item.setId(cloudDB.getBookIndex() + 1);
            item.setBookName(title.getText().toString().trim());
            item.setDescription(description.getText().toString().trim());

            //save the new item to CloudDB
            cloudDB.insertBook(item);
            alertDialog.dismiss();
        });
    }

    /**
     * LogOut
     */
    private void logOut() {
        AGConnectAuth auth = AGConnectAuth.getInstance();
        auth.signOut();

        logOutMenuItem.setVisible(false);
        logInMenuItem.setVisible(true);
    }

    /**
     * LogIn, next step is to obtain info [onActivityResult]
     */
    private void logIn() {
        logOut();
        HuaweiIdAuthParamsHelper huaweiIdAuthParamsHelper = new HuaweiIdAuthParamsHelper(
                HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM);
        List<Scope> scopeList = new ArrayList();
        scopeList.add(new Scope(HwIDConstant.SCOPE.ACCOUNT_BASEPROFILE));
        huaweiIdAuthParamsHelper.setScopeList(scopeList);
        HuaweiIdAuthParams authParams = huaweiIdAuthParamsHelper.setAccessToken().createParams();
        HuaweiIdAuthService service = HuaweiIdAuthManager.getService(this, authParams);
        startActivityForResult(service.getSignInIntent(), HUAWEI_ID_SIGN_IN);

        logInMenuItem.setVisible(false);
        logOutMenuItem.setVisible(true);
    }

    /**
     * Close the CloudDBZone
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        cloudDB.closeCloudDBZone();
    }

    /**
     * Call back to add an item
     */
    @Override
    public void onStart(List<Book> books) {
        items = books;
        itemAdapter = new ItemAdapter(this, (ArrayList<Book>) items);
        itemList.setAdapter(itemAdapter);
        itemAdapter.notifyDataSetChanged();
    }


    @Override
    public void onAddItem(Book book) {
        if (items != null) {
            if (!items.contains(book)) {
                items.add(book);
            }
        }
        itemAdapter.notifyDataSetChanged();
    }
}