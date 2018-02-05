package com.klimczak.digipocket.ui.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.klimczak.digipocket.R;
import com.klimczak.digipocket.core.storage.HierarchicalAddress;
import com.klimczak.digipocket.core.storage.HierarchicalChain;
import com.klimczak.digipocket.core.storage.HierarchicalStorage;
import com.klimczak.digipocket.core.storage.HierarchicalWallet;
import com.klimczak.digipocket.ui.DigiPocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WalletActivity extends AppCompatActivity {

    private static Logger log = LoggerFactory.getLogger(HierarchicalAddress.class);

    HierarchicalStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        DigiPocket pocket =  (DigiPocket) getApplicationContext();
        storage = pocket.getStorage();
        updateChains();
    }


    private void updateChains() {
        HierarchicalWallet wallet = storage.getWallet();
        updateChain(R.id.receive_table, wallet.getReceiveChain());
        updateChain(R.id.change_table, wallet.getChangeChain());
    }

    private void addAddressHeader(TableLayout table) {

        TableRow row = (TableRow) LayoutInflater.from(this).inflate(R.layout.address_table_header, table, false);

        table.addView(row);
    }

    private void addAddressRow(int tableId, int index, TableLayout table, String path, String addr) {

        TableRow row = (TableRow) LayoutInflater.from(this) .inflate(R.layout.address_table_row, table, false);

        row.setTag(tableId);
        row.setId(index);

        TextView tvPath = (TextView) row.findViewById(R.id.row_path);
        tvPath.setText(path);

        TextView tvAddress = (TextView) row.findViewById(R.id.row_addr);
        tvAddress.setText(addr);


        table.addView(row);
    }

    public void handleRowClick(View view) {
        int tableId = (Integer) view.getTag();
        int index = view.getId();
        viewAddress(tableId, index);
    }

    public void viewAddress(int tableId, int index) {
        HierarchicalChain chain = null;
        switch (tableId) {
            case R.id.receive_table:
                log.info(String.format("receive row %d clicked", index));
                chain = storage.getWallet().getReceiveChain();
                break;
            case R.id.change_table:
                log.info(String.format("change row %d clicked", index));
                chain = storage.getWallet().getChangeChain();
                break;
        }

        List<HierarchicalAddress> addrs = chain.getAddresses();
        HierarchicalAddress addr = addrs.get(index);
        String addrstr = addr.getAddressString();

        // Dispatch to the address viewer.
        Intent intent = new Intent(this, ViewAddressActivity.class);
        intent.putExtra("key", addr.getPrivateKeyString());
        intent.putExtra("address", addrstr);
        startActivity(intent);
    }

    private void updateChain(int tableId, HierarchicalChain chain) {


        TableLayout table = (TableLayout) findViewById(tableId);

        table.removeAllViews();

        addAddressHeader(table);

        // Read all of the addresses.  Presume order is correct ...
        int ndx = 0;
        List<HierarchicalAddress> addrs = chain.getAddresses();
        for (HierarchicalAddress addr : addrs) {
            String path = addr.getPath();
            String addrstr = addr.getAbbrev();
            addAddressRow(tableId, ndx++, table, path, addrstr);
        }
    }
}
