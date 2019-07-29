package br.com.luansilveira.savefile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import br.com.luansilveira.savefile.utils.AdapterFileRecycler;
import br.com.luansilveira.savefile.utils.Permissoes;

public class MainActivity extends AppCompatActivity {

    public static final String[] PERMISSOES = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private RecyclerView listViewArquivos;
    private AdapterFileRecycler adapterFile;
    private EditText edNomeArquivo;
    private ImageButton btVoltarPasta;
    private File diretorioAtual;
    private Uri uri;
    private File[] files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewArquivos = findViewById(R.id.listViewArquivos);
        edNomeArquivo = findViewById(R.id.edNomeArquivo);

        if (Permissoes.isPermissoesConcedidas(this, PERMISSOES)) {
            listarArquivos();

            Intent intent = getIntent();

            if (Intent.ACTION_SEND.equals(intent.getAction())){
                this.uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                File arquivo = new File(uri.getPath());

                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(intent.getType());
                String nome = arquivo.getName();
                String nomeCompleto = nome + "." + extension;

                edNomeArquivo.setText(nomeCompleto);
                edNomeArquivo.setSelection(0, nome.length());
                edNomeArquivo.requestFocus();
            }
        }
        else Permissoes.solicitarPermissoes(this, PERMISSOES);
    }

    private void listarArquivos(){
        diretorioAtual = Environment.getExternalStorageDirectory();
        files = diretorioAtual.listFiles();
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        listViewArquivos.setLayoutManager(manager);
        adapterFile = new AdapterFileRecycler(this, files);
        listViewArquivos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        listViewArquivos.setAdapter(adapterFile);

        btVoltarPasta = findViewById(R.id.btVoltarPasta);

        adapterFile.setOnItemClickListener((position) -> {
            File file = adapterFile.getItemAtPosition(position);
            if (file.isDirectory()) {
                atualizarListaArquivos(file);
            } else {
                edNomeArquivo.setText(file.getName());
                edNomeArquivo.setSelection(edNomeArquivo.length());
            }
        });
    }

    public void atualizarListaArquivos(File directory) {
        diretorioAtual = directory;
        this.files = directory.listFiles();
        adapterFile.atualizarLista(files);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int result : grantResults){
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
        } catch (Exception e) {
            e.printStackTrace();
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
        File file = diretorioAtual.getParentFile();
        if (file != null && file.isDirectory()) {
            atualizarListaArquivos(file);
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
}
