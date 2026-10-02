package com.example.gastofacil;

public class HistoryItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TRANSACTION = 1;

    private int type;
    private String headerTitle;
    private TransactionModel transaction;

    public HistoryItem(String headerTitle) {
        this.type = TYPE_HEADER;
        this.headerTitle = headerTitle;
    }

    public HistoryItem(TransactionModel transaction) {
        this.type = TYPE_TRANSACTION;
        this.transaction = transaction;
    }

    public int getType() {
        return type;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public TransactionModel getTransaction() {
        return transaction;
    }
}
