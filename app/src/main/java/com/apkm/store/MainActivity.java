package com.apkm.store;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    StoreCore storeCore;
    ProgressBar carregamento;
    Map<String, DadosPacote> installingPackages = new HashMap<>();
    ViewManager viewManager = new ViewManager();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    void init() {
        viewManager.setLayout(R.layout.store_pagina_inicio);
        storeCore = StoreCore.getInstance();
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int navbarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
            params.setMargins(0, 0, 0, navbarHeight);
            return insets;
        });
        setButtonsFunctions();
        //Adicionando listener de ação de voltar
        getOnBackPressedDispatcher().addCallback(this, OnBack());
    }

    OnBackPressedCallback OnBack() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int currentLayoutID = viewManager.getCurrentLayoutID();
                if (currentLayoutID == R.layout.store_pagina_inicio) {
                    finish();
                } else if (currentLayoutID == R.layout.search_results) {
                    init();
                }
            }
        };
    }

    void setButtonsFunctions() {
        Context main = this;
        Button button = findViewById(R.id.btn_execute_search);
        SearchView searchView = findViewById(R.id.search_bar);
        button.setOnClickListener(v -> {
            String pesquisa = searchView.getQuery().toString();
            searc(main, pesquisa);
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searc(main, query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    void searc(Context context, String seatch) {
        initSearch();
        ListView listView = findViewById(R.id.pesquisas_list); //Lista onde serão inseridos os resultados da pesquisa
        new Thread(() -> {
            try {
                storeCore.searchPackages(context, seatch, new StoreCore.SearchCallback() {
                    @Override
                    public void onSuccess(List<DadosPacote> resultados) {
                        carregamento.setVisibility(View.GONE);

                        // Atualiza o Adapter com os novos dados
                        PesquisasAdapter adapter = new PesquisasAdapter((Activity) context, resultados);
                        runOnUiThread(() -> listView.setAdapter(adapter));
                        new Thread(() -> {
                            while (!isFinishing()) {
                                try {
                                    Thread.sleep(500); // Atualiza a lista 2 vezes por segundo
                                    runOnUiThread(adapter::notifyDataSetChanged);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void onError(String mensagem) {
                        carregamento.setVisibility(View.GONE);
                        Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.i("Erro ao pesquisar", e.getMessage());
            }
        }).start();
    }

    void initSearch() {
        viewManager.setLayout(R.layout.search_results);
        carregamento = findViewById(R.id.resultados_carregando);
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int navbarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
            params.setMargins(0, 0, 0, navbarHeight);
            return insets;
        });
    }

    //Gerenciador de layouts customizado
    class ViewManager {
        int currentLayout = 0;

        int getCurrentLayoutID() {
            return currentLayout;
        }

        void setLayout(int layoutID) {
            setContentView(layoutID);
            currentLayout = layoutID;
        }
    }
}
