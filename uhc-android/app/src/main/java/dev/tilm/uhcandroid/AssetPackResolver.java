package dev.tilm.uhcandroid;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class AssetPackResolver {
    static final class AssetResponse {
        final InputStream stream;
        final String mimeType;
        final int statusCode;
        final String reason;
        final Map<String, String> headers;

        AssetResponse(InputStream stream, String mimeType, int statusCode, String reason,
                      Map<String, String> headers) {
            this.stream = stream;
            this.mimeType = mimeType;
            this.statusCode = statusCode;
            this.reason = reason;
            this.headers = headers;
        }
    }

    private static final class Child {
        final String documentId;
        final String displayName;
        final String mimeType;
        final long size;

        Child(String documentId, String displayName, String mimeType, long size) {
            this.documentId = documentId;
            this.displayName = displayName;
            this.mimeType = mimeType;
            this.size = size;
        }

        boolean isDirectory() {
            return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
        }
    }

    private final ContentResolver resolver;
    private final Uri treeUri;
    private final String rootDocumentId;

    // SAF directory walking is expensive. Cache directory listings and resolved paths.
    private final Map<String, Map<String, Child>> childrenCache =
            Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Child> pathCache =
            Collections.synchronizedMap(new LinkedHashMap<String, Child>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Child> eldest) {
                    return size() > 3000;
                }
            });

    AssetPackResolver(Context context, Uri treeUri) {
        this.resolver = context.getContentResolver();
        this.treeUri = treeUri;
        this.rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
    }

    boolean canReadRoot() {
        try {
            Uri root = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId);
            try (Cursor c = resolver.query(root,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                    null, null, null)) {
                return c != null && c.moveToFirst();
            }
        } catch (Exception e) {
            return false;
        }
    }

    boolean looksLikeUhcPack() {
        try {
            Child archive = find("archive");
            return archive != null && archive.isDirectory();
        } catch (Exception e) {
            return false;
        }
    }

    AssetResponse open(String rawPath, String rangeHeader) throws IOException {
        String path = normalizePath(rawPath);
        Child child = find(path);
        if (child == null || child.isDirectory()) {
            throw new FileNotFoundException(path);
        }

        Uri doc = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId);
        ParcelFileDescriptor pfd = resolver.openFileDescriptor(doc, "r");
        if (pfd == null) throw new FileNotFoundException(path);

        ParcelFileDescriptor.AutoCloseInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        long total = child.size >= 0 ? child.size : pfd.getStatSize();
        String mime = usefulMime(child.mimeType, path);

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Cache-Control", "public, max-age=31536000, immutable");
        headers.put("Accept-Ranges", "bytes");

        if (rangeHeader != null && total > 0 && rangeHeader.startsWith("bytes=")) {
            long start = 0;
            long end = total - 1;
            try {
                String spec = rangeHeader.substring(6).split(",")[0].trim();
                String[] parts = spec.split("-", 2);
                if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
                if (start < 0) start = 0;
                if (end >= total) end = total - 1;
                if (end < start) end = total - 1;

                in.getChannel().position(start);
                long length = end - start + 1;
                headers.put("Content-Range", "bytes " + start + "-" + end + "/" + total);
                headers.put("Content-Length", Long.toString(length));
                return new AssetResponse(new BoundedInputStream(in, length), mime,
                        206, "Partial Content", headers);
            } catch (Exception ignored) {
                try {
                    in.getChannel().position(0);
                } catch (Exception ignored2) {
                }
            }
        }

        if (total >= 0) headers.put("Content-Length", Long.toString(total));
        return new AssetResponse(in, mime, 200, "OK", headers);
    }

    private Child find(String rawPath) throws IOException {
        String path = normalizePath(rawPath);
        if (path.isEmpty()) {
            return new Child(rootDocumentId, "", DocumentsContract.Document.MIME_TYPE_DIR, -1);
        }

        Child cached = pathCache.get(path);
        if (cached != null) return cached;

        String currentId = rootDocumentId;
        Child current = null;
        StringBuilder walked = new StringBuilder();

        for (String encodedPart : path.split("/")) {
            if (encodedPart.isEmpty()) continue;
            String part = Uri.decode(encodedPart);
            Map<String, Child> children = listChildren(currentId);
            current = children.get(part);
            if (current == null) return null;
            currentId = current.documentId;

            if (walked.length() > 0) walked.append('/');
            walked.append(encodedPart);
            pathCache.put(walked.toString(), current);
        }
        return current;
    }

    private Map<String, Child> listChildren(String parentDocumentId) throws IOException {
        Map<String, Child> cached = childrenCache.get(parentDocumentId);
        if (cached != null) return cached;

        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };

        Map<String, Child> result = new HashMap<>();
        try (Cursor c = resolver.query(childrenUri, projection, null, null, null)) {
            if (c == null) throw new IOException("Could not list selected asset directory.");
            while (c.moveToNext()) {
                String id = c.getString(0);
                String name = c.getString(1);
                String mime = c.getString(2);
                long size = c.isNull(3) ? -1 : c.getLong(3);
                if (name != null) result.put(name, new Child(id, name, mime, size));
            }
        } catch (SecurityException e) {
            throw new IOException("Android no longer has permission to read this asset directory.", e);
        }

        childrenCache.put(parentDocumentId, result);
        return result;
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        path = path.replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        while (path.contains("//")) path = path.replace("//", "/");
        return path;
    }

    private static String usefulMime(String mime, String path) {
        if (mime != null && !mime.isEmpty() && !"application/octet-stream".equals(mime)) return mime;
        String p = path.toLowerCase(Locale.US);
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".mp3")) return "audio/mpeg";
        if (p.endsWith(".wav")) return "audio/wav";
        if (p.endsWith(".ogg")) return "audio/ogg";
        if (p.endsWith(".mp4")) return "video/mp4";
        if (p.endsWith(".webm")) return "video/webm";
        if (p.endsWith(".swf")) return "application/x-shockwave-flash";
        if (p.endsWith(".woff")) return "font/woff";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".ttf")) return "font/ttf";
        if (p.endsWith(".txt")) return "text/plain";
        if (p.endsWith(".pdf")) return "application/pdf";
        if (p.endsWith(".epub")) return "application/epub+zip";
        return "application/octet-stream";
    }

    private static final class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        BoundedInputStream(InputStream delegate, long remaining) {
            this.delegate = delegate;
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int r = delegate.read();
            if (r >= 0) remaining--;
            return r;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int want = (int) Math.min(len, remaining);
            int r = delegate.read(b, off, want);
            if (r > 0) remaining -= r;
            return r;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
