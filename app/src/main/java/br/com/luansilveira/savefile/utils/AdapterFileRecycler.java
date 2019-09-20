package br.com.luansilveira.savefile.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.com.luansilveira.savefile.R;

public class AdapterFileRecycler extends RecyclerView.Adapter<AdapterFileRecycler.ViewHolder> {

    private Context context;
    private List<Arquivo> arquivos;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("pt", "BR"));
    private OnItemClickListener listener;

    public AdapterFileRecycler(Context context, Arquivo[] arquivos) {
        this.context = context;
        this.arquivos = Arrays.asList(arquivos);
    }

    public AdapterFileRecycler(Context context, List<Arquivo> arquivos) {
        this.context = context;
        this.arquivos = arquivos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.layout_item_file, parent, false);
        return new ViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.position = position;
        Arquivo arquivo = this.arquivos.get(position);
        long time = System.currentTimeMillis();

        if (arquivo != null) {
            holder.txtNomeArquivo.setText(arquivo.getName());
            Date dataModificacao = new Date((arquivo.lastModified()));
            holder.txtDataModificacao.setText(dateFormat.format(dataModificacao));
            holder.imgIcone.setImageResource(arquivo.getIcone());
            holder.imgIcone.setImageAlpha(arquivo.isHidden() ? 127 : 255);

            String extension = arquivo.getExtension();
            String mimeType = arquivo.getMimeType();
            if ("apk".equals(extension)) {
                Drawable img = getIconFromApk(arquivo);
                if (img != null) holder.imgIcone.setImageDrawable(img);
            } else if (mimeType != null) {
                if (mimeType.contains("image/")) {
                    Picasso.get().load(arquivo).into(holder.imgIcone);
                } else if (mimeType.contains("video/")) {
                    loadVideoThumbnail(arquivo, holder, position);
                }
            }
        }
    }

    private Drawable getIconFromApk(Arquivo arquivo) {
        String apkFilePath = arquivo.getAbsolutePath();
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(apkFilePath, 0);

        if (pi != null) {
            pi.applicationInfo.sourceDir = apkFilePath;
            pi.applicationInfo.publicSourceDir = apkFilePath;

            return pi.applicationInfo.loadIcon(pm);
        }

        return null;
    }

    private Bitmap getVideoThumbnail(File file) {
        return ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
    }

    private void loadVideoThumbnail(Arquivo file, ViewHolder holder, int position) {
        String filename = file.getAbsolutePath();
        Bitmap bitmap = ImageCache.getCache().getFromCache(filename);
        if (bitmap != null) {
            holder.imgIcone.setImageBitmap(bitmap);
        } else {
            holder.imgIcone.setImageResource(R.drawable.ic_video_file);
            holder.videoThumbTask = new ThumbnailTask(filename).setOnLoadListener(b -> {
                holder.imgIcone.setImageBitmap(b);
                notifyItemChanged(position);
            });
            holder.videoThumbTask.execute();
        }
    }

    @Override
    public int getItemCount() {
        return arquivos.size();
    }

    public void atualizarLista(Arquivo[] files) {
        this.arquivos = Arrays.asList(files);
        this.notifyDataSetChanged();
    }

    public void atualizarLista(List<Arquivo> files) {
        this.arquivos = files;
        this.notifyDataSetChanged();
    }

    public Arquivo getItemAtPosition(int position) {
        return arquivos.get(position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onClick(int position);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        if (holder.videoThumbTask != null) {
            if (holder.videoThumbTask.getStatus() == AsyncTask.Status.RUNNING)
                holder.videoThumbTask.cancel(true);
        }
        super.onViewDetachedFromWindow(holder);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        int position;
        ImageView imgIcone;
        TextView txtNomeArquivo;
        TextView txtDataModificacao;
        ThumbnailTask videoThumbTask;

        private OnItemClickListener listener;

        public ViewHolder(View v, OnItemClickListener listener) {
            super(v);

            this.listener = listener;

            imgIcone = v.findViewById(R.id.imgIcone);
            txtNomeArquivo = v.findViewById(R.id.txtNomeArquivo);
            txtDataModificacao = v.findViewById(R.id.txtDataModificacao);

            if (this.listener != null)
                v.setOnClickListener(v1 -> listener.onClick(position));
        }
    }
}
