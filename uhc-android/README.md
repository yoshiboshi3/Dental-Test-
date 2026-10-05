# UHC Android Prototype

Experimental Android wrapper for **The Unofficial Homestuck Collection**.

The APK intentionally contains **no Homestuck Asset Pack**. On first launch it asks Android for read-only, persistent access to an existing UHC Asset Pack folder, then exposes that tree to the bundled reader at a private local origin.

The build job compiles the public UHC web-compatible frontend and bundles only the application code. It removes the analytics script from the generated page and the Android manifest requests no INTERNET permission.

## Prototype goals

- untouched desktop UHC Asset Pack selected with Android's Storage Access Framework
- persistent read-only permission
- fully local WebView frontend and localStorage settings/progress
- range-request support for audio/video
- local Ruffle resources from the UHC frontend
- fold/rotation friendly activity
- no asset-pack conversion step

This is a compatibility experiment. Special pages, Openbound and unusual Flash pages may require follow-up fixes after testing on a real Fold.

UHC credits and licensing remain with the upstream project and its contributors.
