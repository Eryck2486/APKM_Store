package com.apkm.store;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class AppDetails extends AppCompatActivity {
    DadosPacote dados;
    CircularProgressIndicator downloading;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        Intent intent = getIntent();
        String pacoteJSON = intent.getStringExtra("pacote");
        setContentView(R.layout.activity_app_details);
        downloading = findViewById(R.id.app_details_download_progress);
        //Preencher os campos com os dados do pacote
        setAppDetails(pacoteJSON);
        //Adicionando listener de ação de voltar
        getOnBackPressedDispatcher().addCallback(this, OnBack());
        downloadStatus();
        openAppButton();
    }

    void openAppButton() {
        Button button = findViewById(R.id.app_details_open_app_button);
        if (isAppInstalled(dados.getPacote())) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    runOnUiThread(() -> {
                        try {
                            Intent intent = getPackageManager().getLaunchIntentForPackage(dados.getPacote());
                            if (intent != null) {
                                startActivity(intent);
                            } else {
                                String packageS = dados.getPacote().split(":")[1];
                                intent = new Intent(Intent.ACTION_MAIN);
                                intent.setPackage(packageS);
                                ComponentName info = getPackageManager().getLaunchIntentForPackage(packageS).getComponent();
                                if (info != null) intent.setComponent(info);
                                intent.resolveActivity(getPackageManager());
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            Log.e("AppDetails", "Erro ao abrir o aplicativo: " + e.getMessage());
                        }
                    });
                }
            });
            button.setActivated(true);
            button.setVisibility(View.VISIBLE);
        } else {
            button.setActivated(false);
            button.setVisibility(View.INVISIBLE);
        }
    }

    void setAppDetails(String pacoteJSON) {
        dados = DadosPacote.fromJson(pacoteJSON);
        new Thread(() -> {
            if (dados != null) {
                dados.generateIcon(this);
            }
            runOnUiThread(() -> {
                ImageView icon = findViewById(R.id.app_details_icon);
                icon.setImageDrawable(dados.getIcon());
                Button button = findViewById(R.id.app_details_install_button);
                if (isAppInstalled(dados.getPacote())) {
                    button.setText("Desinstalar");
                    button.setOnClickListener(uninstallApp());
                } else {
                    button.setText("Instalar");
                    button.setOnClickListener(instalarApp());
                }
                TextView vendor = findViewById(R.id.app_details_vendor);
                vendor.setText(dados.getAutor());
                TextView version = findViewById(R.id.app_details_version);
                version.setText(dados.getVersao());
                TextView source = findViewById(R.id.app_details_source);
                source.setText(dados.getSource());
                TextView description = findViewById(R.id.app_details_description);
                description.setText(dados.getDescricao());
            });
        }).start();
    }

    View.OnClickListener instalarApp() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    //Solicitar fontes desconhecidas
                    if (!hasInstallPermission(context)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        //Definindo o pacote do app para a solicitação
                        intent.setData(getIntent().getData());
                        startActivity(intent);
                    } else {
                        StoreCore storeCore = StoreCore.getInstance();
                        storeCore.addInstallPackage(dados);
                        downloadStatus();
                        while (storeCore.isDownloading(dados)) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }).start();
            }
        };
    }

    View.OnClickListener uninstallApp() {
        return v -> new Thread(() -> {
            //Desativar o botão de abrir app
            setOpenButtonState(false);
            //Solicitar fontes desconhecidas
            if (!hasInstallPermission(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                //Definindo o pacote do app para a solicitação
                intent.setData(getIntent().getData());
                startActivity(intent);
            } else {
                String pacote;
                if (dados.getPacote().contains(":")) {
                    pacote = dados.getPacote().split(":")[1];
                } else {
                    pacote = dados.getPacote();
                }
                StoreCore storeCore = StoreCore.getInstance();
                storeCore.uninstallPackage(pacote);
                runOnUiThread(() ->
                {
                    Button button = findViewById(R.id.app_details_install_button);
                    button.setText(getString(R.string.install));
                    button.setOnClickListener(instalarApp());
                });
            }
        }).start();
    }

    private void setOpenButtonState(boolean state) {
        int visibility = state ? View.VISIBLE : View.INVISIBLE;
        runOnUiThread(() -> {
            Button button = findViewById(R.id.app_details_open_app_button);
            button.setActivated(state);
            button.setVisibility(visibility);
        });
    }

    boolean isAppInstalled(String packageName) {
        String pacote;
        if (packageName.contains(":")) {
            pacote = packageName.split(":")[1];
        } else {
            pacote = packageName;
        }
        try {
            getPackageManager().getApplicationInfo(pacote, 0);
            // O aplicativo está instalado
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    OnBackPressedCallback OnBack() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
    }

    public boolean hasInstallPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Verifica se o aplicativo tem permissão no Android 8.0+
            return context.getPackageManager().canRequestPackageInstalls();
        }
        // Para versões inferiores, a permissão é sempre verdadeira se ativada globalmente
        return true;
    }

    void downloadStatus() {
        StoreCore storeCore = StoreCore.getInstance();
        new Thread(() -> {
            if (storeCore.isDownloading(dados)) {
                runOnUiThread(() -> {
                    downloading.setActivated(true);
                    downloading.setVisibility(View.VISIBLE);
                    downloading.setMax(100);
                });
                int lastPercent = 0;
                StoreCore.InstallStatus status = storeCore.getDownloadStatus(dados);
                while (storeCore.isDownloading(dados)) {
                    if (lastPercent == 100) {
                        runOnUiThread(() -> {
                            downloading.setProgress(-1);
                            downloading.setIndeterminate(true);
                        });
                    }
                    if (status != null) {
                        int percent = status.getDownloadPercent();
                        if (percent > lastPercent) {
                            lastPercent = percent;
                            runOnUiThread(() -> {
                                downloading.setProgress(percent);
                                Log.i("Progresso", "Percent: " + percent);
                            });
                        }
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(() ->
                {
                    downloading.setVisibility(View.INVISIBLE);
                    Button button = findViewById(R.id.app_details_install_button);
                    button.setText(getString(R.string.uninstall));
                    button.setOnClickListener(uninstallApp());
                });
                //Ativando o botão de abrir app
                setOpenButtonState(true);
            }
        }).start();
    }
}
