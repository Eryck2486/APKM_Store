package com.apkm.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class PesquisasAdapter extends BaseAdapter {
    StoreCore storeCore = StoreCore.getInstance();
    private final Context context;
    private final Activity activity;
    private final List<DadosPacote> pesquisas;

    public PesquisasAdapter(Activity activity, List<DadosPacote> pesquisas) {
        this.context = activity;
        this.activity = activity;
        this.pesquisas = pesquisas;
    }

    @Override
    public int getCount() {
        return pesquisas.size();
    }

    @Override
    public Object getItem(int position) {
        return pesquisas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_search_result, null);
            holder = new ViewHolder();
            holder.icone = convertView.findViewById(R.id.result_appicon);
            holder.nome = convertView.findViewById(R.id.result_appname);
            holder.progress = convertView.findViewById(R.id.progress_app_search_installing);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DadosPacote pacote = pesquisas.get(position);
        holder.nome.setText(pacote.getNome());
        holder.icone.setImageDrawable(pacote.getIcon());
        String pacoteId = pacote.getPacote();

        if (storeCore.isDownloading(pacoteId)) {
            holder.progress.setVisibility(View.VISIBLE);
            StoreCore.InstallStatus status = storeCore.getDownloadStatus(pacoteId);

            if (status != null) {
                int percent = status.getDownloadPercent();
                if (percent >= 100) {
                    holder.progress.setIndeterminate(true);
                } else {
                    holder.progress.setIndeterminate(false);
                    holder.progress.setProgress(percent);
                }
            }
        } else {
            // OBRIGATÓRIO: Se não estiver baixando, esconda e resete
            // Isso impede que o progresso de um item apareça em outro ao rolar
            holder.progress.setVisibility(View.GONE);
            holder.progress.setProgress(0);
            holder.progress.setIndeterminate(false);
        }

        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(activity, AppDetails.class);
            intent.putExtra("pacote", pacote.toJson());
            activity.startActivity(intent);
        });

        String finalPackage;
        if (pacoteId.contains(":")) {
            finalPackage = pacoteId.split(":")[1];
        } else {
            finalPackage = pacoteId;
        }
        if (storeCore.isInstalled(activity, finalPackage)) {
            String blue = "#0000FF";
            holder.progress.setVisibility(View.GONE);
            convertView.setBackgroundColor(Color.parseColor(blue));
        }
        return convertView;
    }

    static class ViewHolder {
        ImageView icone;
        TextView nome;
        ProgressBar progress;
    }
}
