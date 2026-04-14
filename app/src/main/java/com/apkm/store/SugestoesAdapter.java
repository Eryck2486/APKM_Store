package com.apkm.store;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SugestoesAdapter extends BaseAdapter {
    private final Context context;
    private final List<DadosPacote> pacotesSugeridos;

    public SugestoesAdapter(Context context, List<DadosPacote> pacotesSugeridos) {
        this.context = context;
        this.pacotesSugeridos = pacotesSugeridos;
    }

    @Override
    public int getCount() {
        return pacotesSugeridos.size();
    }

    @Override
    public Object getItem(int position) {
        return pacotesSugeridos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_app_sugest, null);
        }
        DadosPacote pacote = pacotesSugeridos.get(position);
        ImageView icone = convertView.findViewById(R.id.sug_appicon);
        TextView nome = convertView.findViewById(R.id.sug_appname);
        icone.setImageDrawable(pacote.getIcon());
        nome.setText(pacote.getNome());
        return convertView;
    }
}
