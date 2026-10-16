# FirePhenix icon resources

The phoenix has its own crest, wing and flame-tail paths surrounding an envelope. The source is not a recolored upstream bird.

- `phoenix-envelope.svg`: editable vector source, 512 by 512 view box.
- `phoenix-envelope.png`: transparent 512 by 512 raster export.
- `phoenix-envelope-android.xml`: Android color vector.
- `phoenix-envelope-monochrome.xml`: Android monochrome vector.

The launcher resources are installed under `app-thunderbird/src/{main,debug,beta,daily}/res/`. Adaptive icons use a 12% inset for launcher masks. Legacy mipmaps cover mdpi through xxxhdpi. Bolt's own-brand logo and the white notification silhouette use the same motif.

Actual small-size and mask inspection: `artifacts/firephenix-nightly/icon-mask-review.png`. Final APK metadata and signatures: `artifacts/firephenix-nightly/checks/apk-badging.log` and `apk-signature.log`. The phone welcome screenshots show the icon rendered by the installed APK.

Upstream attribution and licenses remain with their original owners. This is a derivative Android application, not a claim to a FirePhenix desktop product or Mozilla ownership of this derivative.
