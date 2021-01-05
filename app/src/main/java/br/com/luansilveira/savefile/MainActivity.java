package br.com.luansilveira.savefile;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.luansilveira.savefile.utils.AdapterFileRecycler;
import br.com.luansilveira.savefile.utils.Arquivo;
import br.com.luansilveira.savefile.utils.Permissoes;

public class MainActivity extends AppCompatActivity {

    public static final String[] PERMISSOES = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private final String TAG = "SaveFile";
    private final String FILE_PREFS = "filePrefs";

    private final String ORDER_BY_NAME = "N";
    private final String ORDER_BY_SIZE = "S";
    private final String ORDER_BY_TYPE = "T";
    private final String ORDER_BY_MODIFIED = "M";

    private final String ORDER_ASC = "asc";
    private final String ORDER_DESC = "desc";

    private RecyclerView listViewArquivos;
    private AdapterFileRecycler adapterFile;
    private EditText edNomeArquivo;
    private ImageButton btVoltarPasta;
    private TextView txtPastaVazia;
    private Arquivo diretorioAtual;
    private Button btSalvar;
    private Uri uri;
    private List<Arquivo> arquivos = new ArrayList<>();
    private int parentScrollPosition = 0;
    private String orderBy;
    private String orderDirection;
    private boolean showHidden = false;

    private AlertDialog popup;
    private RadioGroup rgOrdenar;
    private RadioGroup rgOrdem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar bar = getSupportActionBar();

        listViewArquivos = findViewById(R.id.listViewArquivos);
        edNomeArquivo = findViewById(R.id.edNomeArquivo);
        txtPastaVazia = findViewById(R.id.txtPastaVazia);
        listViewArquivos.setHasFixedSize(true);

        carregarPreferencias();

        View viewPopup = LayoutInflater.from(this).inflate(R.layout.layout_ordernar, null);
        rgOrdenar = viewPopup.findViewById(R.id.rgOrdenar);
        rgOrdem = viewPopup.findViewById(R.id.rgOrdem);
        this.popup = new AlertDialog.Builder(this)
                .setPositiveButton("Salvar", this::onAlterarOrdem)
                .setNegativeButton("Cancelar", null)
                .setView(viewPopup)
                .create();

        btSalvar = findViewById(R.id.btSalvar);

        if (Permissoes.isPermissoesConcedidas(this, PERMISSOES)) {
            listarArquivos();

            Intent intent = getIntent();

            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                this.uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                File arquivo = new File(uri.getPath());

                String mimeType = intent.getType();
                Toast.makeText(this, "Mime Type do arquivo recebido: " + mimeType, Toast.LENGTH_LONG).show();
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                String nome = arquivo.getName();
                String nomeCompleto = nome + (extension == null || extension.isEmpty() ? "" : ("." + extension));

                edNomeArquivo.setText(nomeCompleto);
                edNomeArquivo.setSelection(0, nome.length());
                edNomeArquivo.requestFocus();
                btSalvar.setEnabled(true);
            }
        } else Permissoes.solicitarPermissoes(this, PERMISSOES);

        edNomeArquivo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                btSalvar.setEnabled(uri != null && !s.toString().isEmpty());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.visualizarOculto).setChecked(this.showHidden);

        return true;
    }

    private void listarArquivos() {
        diretorioAtual = new Arquivo(Environment.getExternalStorageDirectory());
        arquivos = getArquivos(diretorioAtual);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        listViewArquivos.setLayoutManager(manager);
        adapterFile = new AdapterFileRecycler(this, arquivos);
        listViewArquivos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        listViewArquivos.setAdapter(adapterFile);
        mostrarTextoPastaVazia(arquivos.size() == 0);

        btVoltarPasta = findViewById(R.id.btVoltarPasta);

        mostrarCaminhoDiretorioAtualSubtitulo();

        adapterFile.setOnItemClickListener((position) -> {
            Arquivo file = adapterFile.getItemAtPosition(position);
            if (file.isDirectory()) {
                atualizarListaArquivos(file);
                this.parentScrollPosition = position;
                listViewArquivos.scrollToPosition(0);
            } else {
                edNomeArquivo.setText(file.getName());
                edNomeArquivo.setSelection(edNomeArquivo.length());
            }
        });
    }

    private void mostrarTextoPastaVazia(boolean mostrar) {
        txtPastaVazia.setVisibility(mostrar ? View.VISIBLE : View.GONE);
    }

    private List<Arquivo> getArquivos(File directory) {
        List<Arquivo> list = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {

            for (File file : files) {
                if (!file.isHidden() || this.showHidden) list.add(new Arquivo(file));
            }
        }

        ordenarListaArquivos(list);

        return list;
    }

    public void ordenarListaArquivos(List<Arquivo> lista) {
        Collections.sort(lista, (file1, file2) -> {
            Arquivo arquivo1 = ORDER_ASC.equals(orderDirection) ? file1 : file2;
            Arquivo arquivo2 = ORDER_ASC.equals(orderDirection) ? file2 : file1;

            String extension1 = arquivo1.getExtension() == null ? "" : arquivo1.getExtension();
            String extension2 = arquivo2.getExtension() == null ? "" : arquivo2.getExtension();

            int compareNome = arquivo1.getName().toLowerCase().compareTo(arquivo2.getName().toLowerCase());
            int compareExtension = extension1.toLowerCase().compareTo(extension2.toLowerCase());
            int compareSize = Long.compare(arquivo1.length(), arquivo2.length());
            int compareModified = Long.compare(arquivo1.lastModified(), arquivo2.lastModified());

            switch (this.orderBy) {
                case ORDER_BY_NAME:
                    return compareNome;
                case ORDER_BY_TYPE:
                    return compareExtension == 0 ? compareNome : compareExtension;
                case ORDER_BY_SIZE:
                    return compareSize == 0 ? compareNome : compareSize;
                case ORDER_BY_MODIFIED:
                    return compareModified == 0 ? compareNome : compareModified;
            }

            return 0;
        });
    }

    public void atualizarListaArquivos(Arquivo directory) {
        diretorioAtual = directory;
        this.arquivos = getArquivos(directory);
        adapterFile.atualizarLista(arquivos);
        mostrarTextoPastaVazia(arquivos.size() == 0);

        mostrarCaminhoDiretorioAtualSubtitulo();
    }

    private void mostrarCaminhoDiretorioAtualSubtitulo(){
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setSubtitle(this.diretorioAtual.getAbsolutePath());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) return;
        }

        listarArquivos();

    }

    public void btSalvarClick(View view) {
        String filename = diretorioAtual.getAbsolutePath() + "/" + edNomeArquivo.getText().toString();
        try {
            File novoArquivo = new File(diretorioAtual, edNomeArquivo.getText().toString());
            FileOutputStream outputStream = new FileOutputStream(novoArquivo);
            outputStream.write(getBytes(uri));
            outputStream.close();

            finish();
            Toast.makeText(this, "Arquivo salvo", Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Sem permissão para salvar neste local", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar: \n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void btCancelarClick(View view) {
        finish();
    }

    @Override
    public void onBackPressed() {
        btVoltarPastaClick(btVoltarPasta);
    }

    public void btVoltarPastaClick(View view) {
        Arquivo arquivo = diretorioAtual.getParentFile();
        if (arquivo != null && arquivo.isDirectory()) {
            atualizarListaArquivos(arquivo);
            listViewArquivos.scrollToPosition(this.parentScrollPosition);
        }
    }

    public byte[] getBytes(Uri uri) throws FileNotFoundException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];

        if (inputStream != null) {
            try {
                for (int read; (read = inputStream.read(buf)) != -1; ) {
                    byteArrayOutputStream.write(buf, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    public void btHomeClick(View view) {
        diretorioAtual = new Arquivo(Environment.getExternalStorageDirectory());
        atualizarListaArquivos(diretorioAtual);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.visualizarOculto:
                boolean checked = item.isChecked();
                item.setChecked(this.showHidden = (!checked));
                atualizarListaArquivos(diretorioAtual);
                salvarPreferencias();
                break;
            case R.id.menuOrdenar:
                popup.show();
                rgOrdenar.check(rgOrdenar.findViewWithTag(this.orderBy).getId());
                rgOrdem.check(rgOrdem.findViewWithTag(this.orderDirection).getId());
        }

        return true;
    }

    public void onAlterarOrdem(DialogInterface dialog, int which) {
        RadioButton rbOrdenar = rgOrdenar.findViewById(rgOrdenar.getCheckedRadioButtonId());
        RadioButton rbOrdem = rgOrdem.findViewById(rgOrdem.getCheckedRadioButtonId());

        this.orderBy = rbOrdenar.getTag().toString();
        this.orderDirection = rbOrdem.getTag().toString();
        atualizarListaArquivos(diretorioAtual);
        salvarPreferencias();
    }

    private void salvarPreferencias() {
        SharedPreferences preferences = getSharedPreferences(FILE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("orderBy", this.orderBy).putString("orderDirection", this.orderDirection)
                .putBoolean("showHidden", this.showHidden)
                .apply();
    }

    private void carregarPreferencias() {
        SharedPreferences preferences = getSharedPreferences(FILE_PREFS, Context.MODE_PRIVATE);
        this.orderBy = preferences.getString("orderBy", ORDER_BY_NAME);
        this.orderDirection = preferences.getString("orderDirection", ORDER_ASC);
        this.showHidden = preferences.getBoolean("showHidden", false);
    }

    public void btNovaPastaClick(View view) {
        View vwLayout = LayoutInflater.from(this).inflate(R.layout.layout_nova_pasta_dialog, null);
        EditText editText = vwLayout.findViewById(R.id.edNovaPasta);
        new AlertDialog.Builder(this).setTitle("Nova Pasta")
                .setView(vwLayout)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String nomePasta = editText.getText().toString();
                    Arquivo pasta = new Arquivo(this.diretorioAtual.getAbsolutePath() + '/' + nomePasta);
                    if (pasta.exists()) {
                        new AlertDialog.Builder(this).setTitle("Pasta existente")
                                .setMessage(String.format("O diretório '%s' já existe neste local!", nomePasta))
                                .setPositiveButton("OK", null).show();
                        return;
                    }

                    if (!pasta.mkdir()){
                        Toast.makeText(this, "Erro ao criar diretório neste local!", Toast.LENGTH_LONG).show();
                    }

                    atualizarListaArquivos(pasta);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
