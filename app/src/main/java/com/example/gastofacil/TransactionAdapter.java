package com.example.gastofacil;

import android.graphics.Color;
import android.net.Uri;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionModel> transactionList;
    private NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", new Locale("es", "ES"));
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionModel transaction);
    }

    public TransactionAdapter(List<TransactionModel> transactionList, OnTransactionClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
        format.setMaximumFractionDigits(2);
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionModel transaction = transactionList.get(position);

        holder.tvName.setText(transaction.getCategoria());
        
        // Usar timestamp para formatear la fecha a dd/MM/yy
        String fechaFormateada;
        if (transaction.getTimestamp() > 0) {
            fechaFormateada = dateFormat.format(new Date(transaction.getTimestamp()));
        } else {
            // Fallback por si no hay timestamp (datos antiguos)
            fechaFormateada = transaction.getFecha();
            if (fechaFormateada != null && fechaFormateada.contains("Hoy, ")) {
                fechaFormateada = fechaFormateada.replace("Hoy, ", "");
            }
        }
        
        String descripcion = transaction.getDescripcion();
        if (descripcion != null && descripcion.length() > 10) {
            descripcion = descripcion.substring(0, 10) + "...";
        }
        
        holder.tvDesc.setText(String.format("%s · %s", fechaFormateada, descripcion));

        // Location Logic
        if (transaction.getLatitude() != null && transaction.getLongitude() != null) {
            holder.ivLocation.setVisibility(View.VISIBLE);
            holder.ivLocation.setOnClickListener(v -> {
                try {
                    String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(Ubicación)", 
                            transaction.getLatitude(), transaction.getLongitude(), 
                            transaction.getLatitude(), transaction.getLongitude());
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    // Si falla Maps con el paquete, intentar sin el paquete
                    try {
                        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f", 
                                transaction.getLatitude(), transaction.getLongitude(), 
                                transaction.getLatitude(), transaction.getLongitude());
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        v.getContext().startActivity(intent);
                    } catch (Exception ex) {
                        Toast.makeText(v.getContext(), "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            holder.ivLocation.setVisibility(View.GONE);
        }

        double monto = Double.parseDouble(transaction.getMonto());
        String montoFormateado = CurrencyUtils.formatShort(monto);

        if ("INGRESO".equals(transaction.getTipo())) {
            holder.tvValue.setText("+" + montoFormateado);
            holder.tvValue.setTextColor(Color.parseColor("#4CAF50")); // income_green
            holder.ivIcon.setImageResource(R.drawable.ic_trend_up);
        } else {
            holder.tvValue.setText("-" + montoFormateado);
            holder.tvValue.setTextColor(Color.RED); // expense_red
            holder.ivIcon.setImageResource(R.drawable.ic_trend_down);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvValue;
        ImageView ivIcon, ivLocation;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTransName);
            tvDesc = itemView.findViewById(R.id.tvTransDesc);
            tvValue = itemView.findViewById(R.id.tvTransValue);
            ivIcon = itemView.findViewById(R.id.ivTransIcon);
            ivLocation = itemView.findViewById(R.id.ivLocationIcon);
        }
    }
}
