package br.com.luansilveira.savefile.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public abstract class Permissoes {

    public static void solicitarPermissao(Activity context, String permissao){
        solicitarPermissoes(context, new String[]{permissao});
    }

    public static void solicitarPermissoes(Activity context, String[] permissoes){

        if (permissoes != null) {
            if (permissoes.length > 0) {
                int i = 0;
                String[] permissoesSolicitar = new String[permissoes.length];
                for (String permissao : permissoes){
                    if (!isPermissaoConcedida(context, permissao)) permissoesSolicitar[i++] = permissao;
                }
                ActivityCompat.requestPermissions(context, permissoesSolicitar, 1);
            }
        }
    }

    public static boolean verificarResultadoSolicitacaoPermissoes(int[] results){
        for (int result: results){
            if (result == PackageManager.PERMISSION_DENIED) return false;
        }

        return true;
    }


    public static boolean isPermissaoConcedida(Context context, String permissao){
        return ContextCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isPermissoesConcedidas(Context context, String[] permissoes){
        boolean retorno = true;
        if (permissoes.length == 0) return false;
        for (String permissao : permissoes){
            retorno = (retorno && (isPermissaoConcedida(context, permissao)));
            if (!retorno) break;
        }

        return retorno;
    }

}
