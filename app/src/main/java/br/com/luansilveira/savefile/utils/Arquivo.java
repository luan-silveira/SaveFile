package br.com.luansilveira.savefile.utils;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.net.URI;

import br.com.luansilveira.savefile.R;

public class Arquivo extends File {

    private String extension;
    private String mimeType;
    private int icone;

    public Arquivo(String pathname) {
        super(pathname);
        getExtensionAndMimeType();
    }

    public Arquivo(String parent, String child) {
        super(parent, child);
        getExtensionAndMimeType();
    }

    public Arquivo(File parent, String child) {
        super(parent, child);
        getExtensionAndMimeType();
    }

    public Arquivo(URI uri) {
        super(uri);
        getExtensionAndMimeType();
    }

    public Arquivo(File file) {
        super(file == null ? null : file.getAbsolutePath());
        getExtensionAndMimeType();
    }

    public void getExtensionAndMimeType() {
        this.extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(this).toString());
        this.mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if (isDirectory()) {
            this.icone = R.drawable.ic_directory;
        } else {
            this.icone = R.drawable.ic_unknown_file;

            if (mimeType != null) {
                switch (mimeType.substring(0, mimeType.indexOf("/"))) {
                    case "audio":
                        this.icone = R.drawable.ic_audio_file;
                        break;
                    case "text":
                        this.icone = R.drawable.ic_text_file;
                        break;
                    case "image":
                        this.icone = R.drawable.ic_image_file;
                        break;
                    case "video":
                        this.icone = R.drawable.ic_video_file;
                }
            }
        }
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getIcone() {
        return icone;
    }

    @Override
    public Arquivo getParentFile() {
        return new Arquivo(super.getParentFile());
    }

}
