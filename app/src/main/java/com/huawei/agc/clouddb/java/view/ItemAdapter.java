package com.huawei.agc.clouddb.java.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.agc.clouddb.java.R;
import com.huawei.agc.clouddb.java.model.Book;
import com.huawei.agc.clouddb.java.util.CloudDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private Context context;
    private List<Book> items;
    private CloudDB cloudDB;

    public ItemAdapter(Context context, ArrayList<Book> items) {

        cloudDB = CloudDB.getInstance();
        this.context = context;
        this.items = items;

        cloudDB.createObjectType();
        cloudDB.openCloudDBZone();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new ItemViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView title;
        private TextView desc;
        private Context context;

        public ItemViewHolder(@NonNull View view, @NonNull Context context) {
            super(view);
            title = view.findViewById(R.id.title);
            desc = view.findViewById(R.id.description);
            this.context = context;
            Button editButton = view.findViewById(R.id.editButton);
            Button deleteButton = view.findViewById(R.id.deleteButton);

            editButton.setOnClickListener(this);
            deleteButton.setOnClickListener(this);
        }

        public void bind(Book book) {
            title.setText(book.getBookName());
            desc.setText(book.getDescription());
        }

        @Override
        public void onClick(View view) {
            Book item;

            if (view.getId() == R.id.editButton) {
                item = items.get(getAdapterPosition());
                editItem(item);
            }
            if (view.getId() == R.id.deleteButton) {
                item = items.get(getAdapterPosition());
                deleteItem(item);
            }
        }

        private void deleteItem(Book item) {
            cloudDB.deleteBook(Collections.singletonList(item));
            items.remove(getAdapterPosition());
            notifyItemRemoved(getAdapterPosition());
        }

        private void editItem(Book item) {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.popup, null);

            builder.setView(view);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            Button saveButton = view.findViewById(R.id.saveButton);
            EditText title = view.findViewById(R.id.titleBook);
            EditText description = view.findViewById(R.id.descriptionBook);
            TextView titlePage = view.findViewById(R.id.titlePage);

            titlePage.setText(context.getString(R.string.edit_book));
            title.setText(item.getBookName());
            description.setText(item.getDescription());
            saveButton.setText(context.getString(R.string.update));

            saveButton.setOnClickListener(view1 -> {
                item.setBookName(title.getText().toString().trim());
                item.setDescription(description.getText().toString().trim());

                cloudDB.insertBook(item);
                notifyItemChanged(getAdapterPosition());
                alertDialog.dismiss();
            });

            saveButton.setOnClickListener ( view1 -> {
                item.setBookName(title.getText().toString().trim());
                item.setDescription(description.getText().toString().trim());

                cloudDB.insertBook(item);
                notifyItemChanged(getAdapterPosition());
                alertDialog.dismiss();
            });
        }
    }
}
