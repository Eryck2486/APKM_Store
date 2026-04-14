package com.apkm.store;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreCore {
    private static StoreCore instance;
    private final Map<String, InstallStatus> installMap = new ConcurrentHashMap<>();
    private final List<DadosPacote> searchResults = new ArrayList<>();
    private final ExecutorService serialExecutor = Executors.newSingleThreadExecutor();

    StoreCore() {
    }

    public static synchronized StoreCore getInstance() {
        if (instance == null) {
            instance = new StoreCore();
        }
        return instance;
    }

    public void searchPackages(Context context, String query, SearchCallback callback) {
        // Entra na fila única de execução (serialExecutor)
        serialExecutor.execute(() -> {
            try {
                searchResults.clear();
                // Executa o binário (este método deve ser bloqueante na thread do executor)
                runBinary("apkm search " + query + " -j", null);

                // Pré-processamento: gera os ícones em background antes de avisar a UI
                for (DadosPacote pacote : searchResults) {
                    pacote.generateIcon(context);
                }

                // Sucesso: Volta para a Main Thread para mexer na UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(new ArrayList<>(searchResults));
                });
            } catch (Exception e) {
                // Erro: Avisa a UI sobre a falha
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onError("Falha na busca: " + e.getMessage());
                });
            }
        });
    }

    public InstallStatus getDownloadStatus(DadosPacote pacote) {
        return getDownloadStatus(pacote.getPacote());
    }

    public InstallStatus getDownloadStatus(String pacote) {
        return installMap.get(pacote);
    }

    public boolean isInstalled(Activity act, String pacote) {
        PackageManager pm = act.getPackageManager();
        try {
            pm.getPackageInfo(pacote, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    public boolean isDownloading(DadosPacote pacote) {
        return isDownloading(pacote.getPacote());
    }

    public boolean isDownloading(String pacote) {
        if (pacote != null) {
            return installMap.containsKey(pacote);
        }
        return false;
    }

    public void addInstallPackage(DadosPacote pacote) {
        String id = pacote.getPacote();
        installMap.put(id, new InstallStatus());

        // Entra na fila única. Se houver uma busca ou outro install, ele aguarda.
        serialExecutor.execute(() -> {
            runBinaryInstall("apkm install " + id + " -j", id);
        });
    }

    public void uninstallPackage(String packageName) {
        String command = "apkm uninstall " + packageName + " -j";
        runBinary(command, null);
    }

    private void runBinaryInstall(String command, String packageId) {
        try {
            // Execução direta e bloqueante DENTRO da thread do executor
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = r.readLine()) != null) {
                // Atualiza o progresso no mapa
                updateDownloadProgress(packageId, line);
            }
            p.waitFor(); // Espera o processo morrer de fato antes de liberar a fila
            installMap.remove(packageId);
        } catch (Exception e) {
            Log.e("APKM", "Erro na fila de execução", e);
        }
    }

    private void runBinary(String command, String packageId) {
        try {
            // Execução direta e bloqueante DENTRO da thread do executor
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = r.readLine()) != null) {
                // Atualiza o progresso no mapa
                outputProcessor(command, line, packageId);
            }
            p.waitFor(); // Espera o processo morrer de fato antes de liberar a fila
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateDownloadProgress(String packageId, String line) {
        Gson gson = new Gson();
        if (line.startsWith("\"DOWNLOADING\":")) {
            line = line.replace("\"DOWNLOADING\":", "");
            Downloading download = Downloading.parse(line);
            if ((download != null) && (installMap.containsKey(packageId))) {
                InstallStatus status = installMap.get(packageId);
                if (status != null) {
                    status.setProgress(download);
                }
            }
        }
    }

    private void outputProcessor(String comando, String line, String pacote) {
        Gson gson = new Gson();
        //"DOWNLOADING":{"percent":"0","message":"Iniciando download","size":"54 B"}
        if (comando.startsWith("apkm search")) {
            DadosPacote pacoteTmp = DadosPacote.fromJson(line);
            if (pacoteTmp != null) {
                searchResults.add(pacoteTmp);
            }
        }
    }

    public interface SearchCallback {
        // Chamado quando o apkm termina a busca e o JSON é processado
        void onSuccess(List<DadosPacote> resultados);

        // Chamado se o su falhar, se o binário não existir ou der erro de rede
        void onError(String mensagem);
    }

    //Classes que representam as saídas em json do apkm
    public static class Downloading {
        //"DOWNLOADING":{"percent":"0","message":"Iniciando download","size":"54 B"}
        String percent;
        String message;
        String size;

        public static Downloading parse(String json) {
            Gson gson = new Gson();
            try {
                Downloading download = gson.fromJson(json, Downloading.class);
                if (download != null) {
                    return download;
                }
            } catch (Exception e) {
            }
            return null;
        }

        public int getDownloadPercent() {
            return Integer.parseInt(percent);
        }
    }

    public static class InstallStatus {
        DadosPacote pacote;
        boolean downloading;
        boolean installing;
        int percent;
        String message;

        public void setProgress(Downloading download) {
            percent = download.getDownloadPercent();
            message = download.message;
            downloading = true;
            installing = false;
        }

        public int getDownloadPercent() {
            return percent;
        }
    }

    class Error {
        String message;
        String stacktrace;
    }
}