package br.com.luansilveira.savefile;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

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

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String[] PERMISSOES = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final String ACTION_USB_PERMISSION = "br.com.luansilveira.savefile.ACTION_USB_PERMISSION";

    private static final int REQUEST_CODE_STORAGE_FRAMEWORK = 2;

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

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private UsbManager usbManager;
    private PendingIntent permissionIntent;

    private ArrayMap<Integer, Arquivo> listaArmazenamento = new ArrayMap<>();

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                usbManager.requestPermission(device, permissionIntent);
//                listarDispositivosUSB();
            }

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            listarPastasArmazenamento();
                        }
                    }
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                listarPastasArmazenamento();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewArquivos = findViewById(R.id.listViewArquivos);
        edNomeArquivo = findViewById(R.id.edNomeArquivo);
        txtPastaVazia = findViewById(R.id.txtPastaVazia);
        listViewArquivos.setHasFixedSize(true);

        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 1, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        registerReceiver(usbReceiver, filter);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }

        drawerLayout = findViewById(R.id.drawerLayout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


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
            listarPastasArmazenamento();
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

    private void listarPastasArmazenamento() {
        Menu menu = navigationView.getMenu();
        menu.clear();
        listaArmazenamento.clear();

        menu.add(0, R.id.menuInterno, 0, "Armazenamento interno").setIcon(R.drawable.ic_folder);

        int index = 0;

        String armazenamentoSecundario = System.getenv("SECONDARY_STORAGE");
        Arquivo fileSecundario = new Arquivo(armazenamentoSecundario);
        if (armazenamentoSecundario != null && fileSecundario.exists() && Environment.isExternalStorageRemovable(fileSecundario)) {
            listaArmazenamento.put(index++, fileSecundario);
            menu.add(0, 1, 0, armazenamentoSecundario).setIcon(R.drawable.ic_folder);
        }
        File[] pastas = getExternalFilesDirs(null);
        for (File pasta : pastas) {
            String path = pasta.getAbsolutePath().split("/Android")[0];
            if (!path.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                listaArmazenamento.put(index++, new Arquivo(path));
                menu.add(path).setIcon(R.drawable.ic_folder);
            }
        }

        List<UsbDevice> listUsbDevices = new ArrayList<>(usbManager.getDeviceList().values());
        for (UsbDevice device : listUsbDevices) {
            if (device.getInterface(0).getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE)
                continue;

            menu.add(device.getDeviceName()).setIcon(R.drawable.ic_folder);
        }

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

    private void mostrarCaminhoDiretorioAtualSubtitulo() {
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
        salvarArquivo(filename, false);
    }


    public void salvarArquivo(String filename, boolean viaStorageFramework) {
        try {
            File novoArquivo = new File(filename);
            if (!novoArquivo.createNewFile()) {
                Toast.makeText(this, "O arquivo já existe!", Toast.LENGTH_LONG).show();
                return;
            }

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
            if (!viaStorageFramework) {
                new AlertDialog.Builder(this).setMessage("O sistema não permite salvar neste local. \nEscolha o local de armazenamento externo a seguir")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Ok", (dialogInterface, i) -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE_STORAGE_FRAMEWORK))
                        .show();
            } else {

                Toast.makeText(this, "Erro ao salvar: \n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void btCancelarClick(View view) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_STORAGE_FRAMEWORK && resultCode == RESULT_OK) {
            String filename = data.getData().getPath() + "/" + edNomeArquivo.getText().toString();
            salvarArquivo(filename, true);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isOpen()) drawerLayout.closeDrawer(GravityCompat.START);
        else btVoltarPastaClick(btVoltarPasta);
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
        atualizarListaArquivos(new Arquivo(Environment.getExternalStorageDirectory()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                if (!drawerLayout.isOpen()) drawerLayout.openDrawer(GravityCompat.START);
                else drawerLayout.closeDrawer(GravityCompat.START);

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

        return super.onOptionsItemSelected(item);
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

                    if (!pasta.mkdir()) {
                        Toast.makeText(this, "Erro ao criar diretório neste local!", Toast.LENGTH_LONG).show();
                    }

                    atualizarListaArquivos(pasta);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Arquivo arquivo;
        if (item.getItemId() == R.id.menuInterno) {
            arquivo = new Arquivo(Environment.getExternalStorageDirectory());
        } else {
            arquivo = new Arquivo(item.getTitle().toString());
        }

        atualizarListaArquivos(arquivo);

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
