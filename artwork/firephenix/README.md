# FirePhenix icon resources — phoenix carrying mail

The current icon is a red, orange and gold phoenix holding an ivory envelope in its beak. Both wings and flame-tail feathers remain visible. The launcher, welcome and About logo now use the colored Liquid Glass variant (`phoenix-carrying-mail-glass.png`); the original color image is retained as artwork provenance. The white silhouette is reserved for Android themed icons and monochrome notifications.

- `phoenix-carrying-mail.png`: selected color artwork, 1254 × 1254, with alpha.
- `phoenix-carrying-mail-monochrome.png`: matching Android monochrome artwork, with alpha.
- `generation-v2.json`: built-in image generation provenance and source hashes.
- `generation-spec-v2.md`: generation requirements summary; original verbatim prompts were not retained.
- `previous-v1/` and the older `phoenix-envelope*` files preserve the former vector design; they are not the new raster icon source.

The unified launcher resources are under `app-thunderbird/src/main/res/`; variant-specific legacy icons were removed. Adaptive icons use a 16% foreground inset. Legacy mipmaps cover mdpi through xxxhdpi. Resource exports only resize the generated artwork; the original alpha is preserved. Bolt's logo and notification silhouette use the same phoenix identity.

Current mechanical mask and 48/32/24 px review: `artifacts/firephenix-liquid-glass/icon-mask-review-v2.png`. This preview does not itself prove device installation; APK and device verification are recorded separately in the current delivery checks. The old nightly mask review applies only to the former icon.

Upstream attribution and licenses remain with their original owners. This is a derivative Android application, not a claim to a FirePhenix desktop product or Mozilla ownership of this derivative.

Generation metadata retains prompts and source hashes but omits absolute workstation paths. Local mask reviews and device screenshots under `artifacts/` are intentionally excluded from Git.
