package com.example.navigationview;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
public class SuperAdminAdapter extends RecyclerView.Adapter<SuperAdminAdapter.ViewHolder>{

    Context context;
    List<DocumentSnapshot> list;

    public SuperAdminAdapter(Context context,List<DocumentSnapshot> list){
        this.context=context;
        this.list=list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType){

        View view = LayoutInflater.from(context)
                .inflate(R.layout.admin_item,parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,int position){

        DocumentSnapshot doc = list.get(position);

        String name = doc.getString("name");
        String farm = doc.getString("farmName");



        holder.adminName.setText(name);
        holder.farmName.setText(farm);

        String uid = doc.getId();

        FirebaseFirestore.getInstance()
                .collection("Admins")
                .document(uid)
                .collection("Customers")
                .get()
                .addOnSuccessListener(q -> {

                    holder.customerCount.setText(
                            "Customers : " + q.size());

                });

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(context,
                    AdminProfileActivity.class);

            intent.putExtra("adminUID",uid);

            context.startActivity(intent);

        });

    }

    @Override
    public int getItemCount(){
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView adminName,farmName,customerCount;

        public ViewHolder(View itemView){
            super(itemView);

            adminName = itemView.findViewById(R.id.adminName);
            farmName = itemView.findViewById(R.id.farmName);
            customerCount = itemView.findViewById(R.id.customerCount);
        }
    }
}
