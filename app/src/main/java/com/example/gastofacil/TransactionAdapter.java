package com.example.gastofacil;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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
        ImageView ivIcon;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTransName);
            tvDesc = itemView.findViewById(R.id.tvTransDesc);
            tvValue = itemView.findViewById(R.id.tvTransValue);
            ivIcon = itemView.findViewById(R.id.ivTransIcon);
        }
    }
}
