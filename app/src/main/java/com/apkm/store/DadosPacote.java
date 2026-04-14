package com.apkm.store;

import static androidx.appcompat.content.res.AppCompatResources.getDrawable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

public class DadosPacote {
    private final String nome;
    private final String versao;
    private final String autor;
    private final String descricao;
    private final String icon;
    private final String pacote;
    private String source;
    private transient Drawable icone;

    //Construtor
    public DadosPacote(Context context, String pacote, String nome, String versao, String autor, String descricao, String icon) {
        this.nome = nome;
        this.versao = versao;
        this.autor = autor;
        this.descricao = descricao;
        this.icon = icon;
        this.pacote = pacote;
    }

    public static DadosPacote fromJson(String json) {
        Log.i("JSON", json);
        Gson gson = new Gson();
        DadosPacote pacote = gson.fromJson(json, DadosPacote.class);
        if (pacote == null) {
            Log.i("Erro ao converter", "Pacote nulo");
            return null;
        }
        return pacote;
    }

    //Retorna o ID do pacote
    public String getPacote() {
        return pacote;
    }

    //Retorna o nome do pacote
    public String getNome() {
        return nome;
    }

    //Retorna a versão do pacote
    public String getVersao() {
        return versao;
    }

    //Retorna o autor do pacote
    public String getAutor() {
        return autor;
    }

    //Retorna a descrição do pacote
    public String getDescricao() {
        return descricao;
    }

    //Converter base64 para Drawable
    public Drawable getIcon() {
        return icone;
    }

    public String getSource() {
        return source;
    }

    public void generateIcon(Context context) {
        //Icone do app
        Drawable iconApp = getDrawable(context, R.drawable.ic_launcher_foreground);
        //Obtendo o Drawable através do URL na variável icone através do Glide
        try {
            iconApp = Glide.with(context)
                    .asDrawable()
                    .load(icon)
                    .submit()
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        icone = iconApp;
    }

    public String toJson() {
        try {
            Gson gson = new Gson();
            return gson.toJson(this);
        } catch (Exception e) {
            Log.i("Erro ao converter", e.getMessage());
            return "";
        }
    }
}
