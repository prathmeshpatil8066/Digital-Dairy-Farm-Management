package com.example.navigationview;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.*;

import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.MyViewHolder> {

    List<DocumentSnapshot> list;
    Context context;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AdminAdapter(List<DocumentSnapshot> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(R.layout.admin_item,parent,false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        DocumentSnapshot doc = list.get(position);

        holder.tvName.setText(doc.getString("name"));
        holder.tvEmail.setText(doc.getString("email"));
        holder.tvMobile.setText(doc.getString("mobile"));

        holder.btnDisable.setOnClickListener(v -> {

            db.collection("Admins")
                    .document(doc.getId())
                    .update("active",false);

            Toast.makeText(context,
                    "Admin Disabled",
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView tvName,tvEmail,tvMobile;
        Button btnDisable;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvMobile = itemView.findViewById(R.id.tvMobile);
        }
    }
}