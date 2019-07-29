package br.com.luansilveira.savefile.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.com.luansilveira.savefile.R;

public class AdapterFileRecycler extends RecyclerView.Adapter<AdapterFileRecycler.ViewHolder> {

    private final int SIZE_THUMB = 120;
    private Context context;
    private File[] files;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("pt", "BR"));
    private OnItemClickListener listener;

    public AdapterFileRecycler(Context context, File[] files) {
        this.context = context;
        this.files = files;
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
        File file = this.files[position];
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if (file != null) {
            holder.txtNomeArquivo.setText(file.getName());
            Date dataModificacao = new Date((file.lastModified()));
            holder.txtDataModificacao.setText(dateFormat.format(dataModificacao));
            if (file.isDirectory()) {
                holder.imgIcone.setImageResource(R.drawable.ic_directory);
            } else {
                int icon = R.drawable.ic_unknown_file;
                if (mimeType != null) {
                    switch (mimeType.substring(0, mimeType.indexOf("/"))) {
                        case "audio":
                            icon = R.drawable.ic_audio_file;
                            break;
                        case "text":
                            icon = R.drawable.ic_text_file;
                            break;
                        case "image":
                            icon = R.drawable.ic_image_file;
                            Picasso.get().load(file).into(holder.imgIcone);
                            break;
                        case "video":
                            icon = R.drawable.ic_video_file;
                    }

                }
                holder.imgIcone.setImageResource(icon);
            }
        }
    }

    private Bitmap getVideoThumbnail(File file) {
        return ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
    }

    private Bitmap getThumbnail(@DrawableRes int res) {
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), res);
        return getThumbnail(bmp);
    }

    private Bitmap getThumbnail(Bitmap bmp) {
        return ThumbnailUtils.extractThumbnail(bmp, SIZE_THUMB, SIZE_THUMB);
    }


    @Override
    public int getItemCount() {
        return files.length;
    }

    public void atualizarLista(File[] files) {
        this.files = files;
        this.notifyDataSetChanged();
    }

    public File getItemAtPosition(int position) {
        try {
            return files[position];
        } catch (Exception e) {
            return null;
        }
    }

    public AdapterFileRecycler setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnItemClickListener {
        void onClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        int position;
        ImageView imgIcone;
        TextView txtNomeArquivo;
        TextView txtDataModificacao;

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
